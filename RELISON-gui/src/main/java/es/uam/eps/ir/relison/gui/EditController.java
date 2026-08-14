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
import es.uam.eps.ir.relison.graph.multigraph.MultiGraph;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;

/**
 * REST handlers for Gephi-style interactive editing of the loaded network: adding and removing nodes and edges.
 *
 * <p>Every successful mutation invalidates the session caches ({@link GraphSession#invalidateCaches()}) so that the
 * next metric or community computation runs against the edited graph, and returns the updated node/edge counts so
 * the frontend can patch the sigma graph in place.</p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class EditController
{
    private final GraphStore store;

    public EditController(GraphStore store)
    {
        this.store = store;
    }

    /**
     * Handles {@code POST /api/graph/{id}/node}: adds a node.
     * @param ctx the request context, with body {@code {node}}.
     */
    public void addNode(Context ctx)
    {
        GraphSession session = session(ctx);
        if (session == null) return;
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        String node = parseNode(ctx, body.get("node"));
        if (node == null) return;

        boolean added = session.getGraph().addNode(node);
        finish(ctx, session, added, "Node already exists or could not be added.");
    }

    /**
     * Handles {@code DELETE /api/graph/{id}/node/{node}}: removes a node and its incident edges.
     * @param ctx the request context.
     */
    public void removeNode(Context ctx)
    {
        GraphSession session = session(ctx);
        if (session == null) return;
        String node = parseNode(ctx, ctx.pathParam("node"));
        if (node == null) return;

        boolean removed = session.getGraph().removeNode(node);
        finish(ctx, session, removed, "Node does not exist.");
    }

    /**
     * Handles {@code POST /api/graph/{id}/node/rename}: changes a node's identifier, keeping its edges and
     * attributes. Caches are invalidated because previously-computed results were keyed by the old identifier.
     * @param ctx the request context, with body {@code {old, new}}.
     */
    public void renameNode(Context ctx)
    {
        GraphSession session = session(ctx);
        if (session == null) return;
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        String oldId = parseNode(ctx, body.get("old"));
        String newId = parseNode(ctx, body.get("new"));
        if (oldId == null || newId == null) return;

        boolean renamed = session.getGraph().renameNode(oldId, newId);
        finish(ctx, session, renamed, "Could not rename the node (it may not exist, or the new id is already taken).");
    }

    /**
     * Handles {@code POST /api/graph/{id}/edge}: adds an edge (creating the endpoints if needed).
     * @param ctx the request context, with body {@code {source, target, weight?}}.
     */
    public void addEdge(Context ctx)
    {
        GraphSession session = session(ctx);
        if (session == null) return;
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        String source = parseNode(ctx, body.get("source"));
        String target = parseNode(ctx, body.get("target"));
        if (source == null || target == null) return;

        double weight = body.get("weight") == null ? 1.0 : Double.parseDouble(String.valueOf(body.get("weight")));
        boolean added = session.getGraph().addEdge(source, target, weight);
        finish(ctx, session, added, "Edge could not be added.");
    }

    /**
     * Handles {@code DELETE /api/graph/{id}/edge}: removes an edge.
     * @param ctx the request context, with body {@code {source, target}}.
     */
    public void removeEdge(Context ctx)
    {
        GraphSession session = session(ctx);
        if (session == null) return;
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        String source = parseNode(ctx, body.get("source"));
        String target = parseNode(ctx, body.get("target"));
        if (source == null || target == null) return;

        Graph<String> graph = session.getGraph();
        boolean removed;
        // For a multigraph, an edge id removes one specific parallel edge; otherwise remove the pair.
        String edgeId = body.get("edgeId") == null ? null : String.valueOf(body.get("edgeId")).trim();
        if (edgeId != null && !edgeId.isEmpty() && graph instanceof MultiGraph)
        {
            MultiGraph<String> mg = (MultiGraph<String>) graph;
            List<Long> ids = mg.getEdgeIds(source, target);
            int idx = ids.indexOf(Long.parseLong(edgeId));
            removed = idx >= 0 && mg.removeEdge(source, target, idx);
        }
        else
        {
            removed = graph.removeEdge(source, target);
        }
        finish(ctx, session, removed, "Edge does not exist.");
    }

    /**
     * Handles {@code POST /api/graph/{id}/edges}: adds many edges in a single request (endpoints are created as
     * needed). Backs the "add all recommended links" action, which would otherwise need one request per link.
     * @param ctx the request context, with body {@code {edges:[{source,target,weight?}]}}.
     */
    public void addEdges(Context ctx)
    {
        bulkEdges(ctx, true);
    }

    /**
     * Handles {@code DELETE /api/graph/{id}/edges}: removes many edges in a single request (the inverse of
     * {@link #addEdges(Context)}, used to undo a bulk addition).
     * @param ctx the request context, with body {@code {edges:[{source,target}]}}.
     */
    public void removeEdges(Context ctx)
    {
        bulkEdges(ctx, false);
    }

    /**
     * Applies a batch of edge additions or removals, reporting how many actually took effect. Individual entries that
     * cannot be applied (a duplicate on add, a missing edge on remove) are skipped rather than failing the batch, so a
     * partially-stale request still does as much as it can.
     * @param ctx the request context.
     * @param add {@code true} to add the edges, {@code false} to remove them.
     */
    private void bulkEdges(Context ctx, boolean add)
    {
        GraphSession session = session(ctx);
        if (session == null) return;
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        Object raw = body.get("edges");
        if (!(raw instanceof List))
        {
            ctx.status(400).json(Map.of("error", "Missing 'edges' list."));
            return;
        }

        Graph<String> graph = session.getGraph();
        int applied = 0;
        for (Object o : (List<?>) raw)
        {
            if (!(o instanceof Map)) continue;
            Map<?, ?> e = (Map<?, ?>) o;
            String source = token(e.get("source"));
            String target = token(e.get("target"));
            if (source == null || target == null) continue;
            if (add)
            {
                double weight;
                try { weight = e.get("weight") == null ? 1.0 : Double.parseDouble(String.valueOf(e.get("weight"))); }
                catch (NumberFormatException ex) { weight = 1.0; }
                if (graph.addEdge(source, target, weight)) applied++;
            }
            else if (graph.removeEdge(source, target))
            {
                applied++;
            }
        }

        if (applied > 0) session.invalidateCaches();
        ctx.json(Map.of(
                "ok", true,
                "applied", applied,
                "stats", Map.of("nodes", graph.getVertexCount(), "edges", graph.getEdgeCount())
        ));
    }

    /* ------------------------------ helpers ------------------------------ */

    /** A non-blank node identifier from a raw JSON value, or {@code null}. */
    private String token(Object raw)
    {
        if (raw == null) return null;
        String id = String.valueOf(raw).trim();
        return id.isEmpty() ? null : id;
    }

    private GraphSession session(Context ctx)
    {
        GraphSession session = store.get(ctx.pathParam("id"));
        if (session == null)
        {
            ctx.status(404).json(Map.of("error", "Unknown graph id."));
            return null;
        }
        return session;
    }

    private String parseNode(Context ctx, Object raw)
    {
        if (raw == null)
        {
            ctx.status(400).json(Map.of("error", "Missing node identifier."));
            return null;
        }
        // Node identifiers are arbitrary strings; accept any non-blank token (numbers arrive as JSON numbers).
        String id = String.valueOf(raw).trim();
        if (id.isEmpty())
        {
            ctx.status(400).json(Map.of("error", "Node identifier must not be blank."));
            return null;
        }
        return id;
    }

    private void finish(Context ctx, GraphSession session, boolean success, String failureMessage)
    {
        if (!success)
        {
            ctx.status(400).json(Map.of("error", failureMessage));
            return;
        }
        session.invalidateCaches();
        Graph<String> graph = session.getGraph();
        ctx.json(Map.of(
                "ok", true,
                "stats", Map.of("nodes", graph.getVertexCount(), "edges", graph.getEdgeCount())
        ));
    }
}
