/*
 *  Copyright (C) 2023 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.gui;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.attributes.AttributeType;
import es.uam.eps.ir.relison.graph.multigraph.MultiGraph;
import es.uam.eps.ir.relison.io.attributes.EdgeAttributeReader;
import es.uam.eps.ir.relison.io.attributes.NodeAttributeReader;

import java.util.List;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import org.ranksys.formats.parsing.Parsers;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST handlers for node and edge attributes: importing them from sidecar files, declaring new ones, and editing
 * individual values. Attributes do not affect metric computation, so these operations do not invalidate the session
 * caches.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class AttributeController
{
    private final GraphStore store;

    public AttributeController(GraphStore store)
    {
        this.store = store;
    }

    /**
     * Handles {@code POST /api/graph/{id}/attributes/nodes}: imports node attributes from an uploaded sidecar file.
     * @param ctx the request context (multipart field {@code file}).
     */
    public void uploadNodes(Context ctx)
    {
        GraphSession session = session(ctx);
        if (session == null) return;
        UploadedFile file = ctx.uploadedFile("file");
        if (file == null)
        {
            ctx.status(400).json(Map.of("error", "No file was uploaded (expected multipart field 'file')."));
            return;
        }
        try (InputStream in = file.content())
        {
            boolean ok = new NodeAttributeReader<>("\t", Parsers.sp).read(session.getGraph(), in);
            if (!ok)
            {
                ctx.status(400).json(Map.of("error", "Could not parse the node attribute file."));
                return;
            }
        }
        catch (Exception e)
        {
            ctx.status(400).json(Map.of("error", "Could not parse the node attribute file: " + e.getMessage()));
            return;
        }
        respondWithGraph(ctx, session);
    }

    /**
     * Handles {@code POST /api/graph/{id}/attributes/edges}: imports edge attributes from an uploaded sidecar file.
     * @param ctx the request context (multipart field {@code file}).
     */
    public void uploadEdges(Context ctx)
    {
        GraphSession session = session(ctx);
        if (session == null) return;
        UploadedFile file = ctx.uploadedFile("file");
        if (file == null)
        {
            ctx.status(400).json(Map.of("error", "No file was uploaded (expected multipart field 'file')."));
            return;
        }
        try (InputStream in = file.content())
        {
            boolean ok = new EdgeAttributeReader<>("\t", Parsers.sp).read(session.getGraph(), in);
            if (!ok)
            {
                ctx.status(400).json(Map.of("error", "Could not parse the edge attribute file."));
                return;
            }
        }
        catch (Exception e)
        {
            ctx.status(400).json(Map.of("error", "Could not parse the edge attribute file: " + e.getMessage()));
            return;
        }
        respondWithGraph(ctx, session);
    }

    /**
     * Handles {@code POST /api/graph/{id}/attributes/define}: declares a new node or edge attribute.
     * @param ctx the request context, with body {@code {target:"node"|"edge", name, type}}.
     */
    public void define(Context ctx)
    {
        GraphSession session = session(ctx);
        if (session == null) return;
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        String target = String.valueOf(body.get("target"));
        String name = body.get("name") == null ? "" : String.valueOf(body.get("name")).trim();
        if (name.isEmpty())
        {
            ctx.status(400).json(Map.of("error", "Attribute name must not be blank."));
            return;
        }
        AttributeType type;
        try
        {
            type = AttributeType.fromToken(String.valueOf(body.get("type")));
        }
        catch (IllegalArgumentException e)
        {
            ctx.status(400).json(Map.of("error", e.getMessage()));
            return;
        }
        Graph<String> graph = session.getGraph();
        if ("edge".equalsIgnoreCase(target)) graph.defineEdgeAttribute(name, type);
        else graph.defineNodeAttribute(name, type);
        ctx.json(Map.of("ok", true, "schema", GraphSerializer.schema(session)));
    }

    /**
     * Handles {@code POST /api/graph/{id}/attributes/remove}: deletes a node or edge attribute and all its values.
     * @param ctx the request context, with body {@code {target:"node"|"edge", name}}.
     */
    public void remove(Context ctx)
    {
        GraphSession session = session(ctx);
        if (session == null) return;
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        String target = String.valueOf(body.get("target"));
        String name = str(body.get("name"));
        if (name == null)
        {
            ctx.status(400).json(Map.of("error", "Attribute name must not be blank."));
            return;
        }
        Graph<String> graph = session.getGraph();
        boolean removed = "edge".equalsIgnoreCase(target) ? graph.removeEdgeAttribute(name) : graph.removeNodeAttribute(name);
        ctx.json(Map.of("ok", removed, "schema", GraphSerializer.schema(session)));
    }

    /**
     * Handles {@code POST /api/graph/{id}/attributes/node}: sets a node attribute value.
     * @param ctx the request context, with body {@code {node, name, value}}.
     */
    public void setNode(Context ctx)
    {
        GraphSession session = session(ctx);
        if (session == null) return;
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        Graph<String> graph = session.getGraph();
        String node = str(body.get("node"));
        String name = str(body.get("name"));
        AttributeType type = name == null ? null : graph.getNodeAttributeType(name);
        if (type == null)
        {
            ctx.status(400).json(Map.of("error", "Unknown node attribute: " + name));
            return;
        }
        try
        {
            boolean ok = graph.setNodeAttribute(node, name, coerce(type, body.get("value")));
            if (!ok)
            {
                ctx.status(400).json(Map.of("error", "Could not set the attribute (does the node exist?)."));
                return;
            }
        }
        catch (IllegalArgumentException e)
        {
            ctx.status(400).json(Map.of("error", e.getMessage()));
            return;
        }
        ctx.json(Map.of("ok", true));
    }

    /**
     * Handles {@code POST /api/graph/{id}/attributes/edge}: sets an edge attribute value.
     * @param ctx the request context, with body {@code {source, target, name, value}}.
     */
    public void setEdge(Context ctx)
    {
        GraphSession session = session(ctx);
        if (session == null) return;
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        Graph<String> graph = session.getGraph();
        String source = str(body.get("source"));
        String dest = str(body.get("target"));
        String name = str(body.get("name"));
        AttributeType type = name == null ? null : graph.getEdgeAttributeType(name);
        if (type == null)
        {
            ctx.status(400).json(Map.of("error", "Unknown edge attribute: " + name));
            return;
        }
        try
        {
            Object value = coerce(type, body.get("value"));
            boolean ok;
            // For a multigraph, an edge id targets one specific parallel edge; otherwise set on the pair.
            String edgeId = str(body.get("edgeId"));
            if (edgeId != null && graph instanceof MultiGraph)
            {
                MultiGraph<String> mg = (MultiGraph<String>) graph;
                List<Long> ids = mg.getEdgeIds(source, dest);
                int idx = ids.indexOf(Long.parseLong(edgeId));
                ok = idx >= 0 && mg.setEdgeAttribute(source, dest, idx, name, value);
            }
            else
            {
                ok = graph.setEdgeAttribute(source, dest, name, value);
            }
            if (!ok)
            {
                ctx.status(400).json(Map.of("error", "Could not set the attribute (does the edge exist?)."));
                return;
            }
        }
        catch (IllegalArgumentException e)
        {
            ctx.status(400).json(Map.of("error", e.getMessage()));
            return;
        }
        ctx.json(Map.of("ok", true));
    }

    /* ------------------------------ helpers ------------------------------ */

    /** Serializes the (updated) graph and schema after a bulk attribute import. */
    private void respondWithGraph(Context ctx, GraphSession session)
    {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        response.put("graph", GraphSerializer.toGraphology(session));
        response.put("schema", GraphSerializer.schema(session));
        ctx.json(response);
    }

    /**
     * Coerces a raw JSON value to the Java type expected by an attribute. A {@code null} or empty value clears it.
     * @param type the declared attribute type.
     * @param raw  the raw value from the request body.
     * @return the coerced value, or {@code null} to clear the attribute.
     */
    private Object coerce(AttributeType type, Object raw)
    {
        if (raw == null) return null;
        if (raw instanceof String && ((String) raw).isBlank()) return null;
        return switch (type)
        {
            case INTEGER -> raw instanceof Number ? ((Number) raw).intValue() : Integer.valueOf(raw.toString().trim());
            case LONG -> raw instanceof Number ? ((Number) raw).longValue() : Long.valueOf(raw.toString().trim());
            case DOUBLE -> raw instanceof Number ? ((Number) raw).doubleValue() : Double.valueOf(raw.toString().trim());
            case BOOLEAN -> raw instanceof Boolean ? raw : AttributeType.BOOLEAN.parse(raw.toString());
            case STRING, CATEGORICAL -> raw.toString();
        };
    }

    private static String str(Object o)
    {
        return o == null ? null : String.valueOf(o).trim();
    }

    private GraphSession session(Context ctx)
    {
        GraphSession session = store.get(ctx.pathParam("id"));
        if (session == null)
        {
            ctx.status(404).json(Map.of("error", "Unknown graph id."));
        }
        return session;
    }
}
