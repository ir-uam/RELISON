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
import es.uam.eps.ir.relison.io.graph.GraphReader;
import es.uam.eps.ir.relison.io.graph.TextGraphReader;
import es.uam.eps.ir.relison.io.graph.TextMultiGraphReader;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import org.ranksys.formats.parsing.Parsers;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST handlers for loading a network and reading it back in graphology format.
 *
 * <p>Mirrors the graph-reading logic of {@code GraphAnalyzer}: a {@link TextGraphReader} (or
 * {@link TextMultiGraphReader} for multigraphs) parses a tab-separated edge list. Node identifiers are read as
 * {@link String} values, so the GUI accepts arbitrary id types (numeric or textual) without configuration.</p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class GraphController
{
    private final GraphStore store;

    /**
     * Constructor.
     * @param store the registry of loaded networks.
     */
    public GraphController(GraphStore store)
    {
        this.store = store;
    }

    /**
     * Handles {@code POST /api/graph/load}: a multipart upload of a tab-separated edge list plus the network type
     * flags. Reads the graph, registers a session, and returns the session id, basic stats and the serialized graph.
     * @param ctx the request context.
     */
    public void load(Context ctx)
    {
        UploadedFile file = ctx.uploadedFile("file");
        if (file == null)
        {
            ctx.status(400).json(Map.of("error", "No file was uploaded (expected multipart field 'file')."));
            return;
        }

        boolean directed = boolParam(ctx, "directed", true);
        boolean weighted = boolParam(ctx, "weighted", false);
        boolean multigraph = boolParam(ctx, "multigraph", false);
        boolean selfloops = boolParam(ctx, "selfloops", false);

        GraphReader<String> reader = multigraph
                ? new TextMultiGraphReader<>(directed, weighted, selfloops, "\t", Parsers.sp)
                : new TextGraphReader<>(directed, weighted, selfloops, "\t", Parsers.sp);

        Graph<String> graph;
        try (InputStream in = file.content())
        {
            graph = reader.read(in, weighted, false);
        }
        catch (Exception e)
        {
            ctx.status(400).json(Map.of("error", "Could not parse the graph: " + e.getMessage()));
            return;
        }

        if (graph == null)
        {
            ctx.status(400).json(Map.of("error", "The graph could not be read. Check the file format and the type flags."));
            return;
        }

        String id = store.newId();
        GraphSession session = new GraphSession(id, graph, directed, weighted, multigraph, selfloops);
        store.register(session);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("graphId", id);
        response.put("stats", GraphSerializer.stats(session));
        response.put("graph", GraphSerializer.toGraphology(session));
        response.put("schema", GraphSerializer.schema(session));
        ctx.json(response);
    }

    /**
     * Handles {@code GET /api/graph/{id}}: returns the network in graphology format.
     * @param ctx the request context.
     */
    public void get(Context ctx)
    {
        GraphSession session = store.get(ctx.pathParam("id"));
        if (session == null)
        {
            ctx.status(404).json(Map.of("error", "Unknown graph id."));
            return;
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("graphId", session.getId());
        response.put("stats", GraphSerializer.stats(session));
        response.put("graph", GraphSerializer.toGraphology(session));
        response.put("schema", GraphSerializer.schema(session));
        ctx.json(response);
    }

    /**
     * Reads a boolean multipart/form field, falling back to a default when absent.
     * @param ctx the request context.
     * @param name the field name.
     * @param def  the default value.
     * @return the parsed boolean.
     */
    static boolean boolParam(Context ctx, String name, boolean def)
    {
        String value = ctx.formParam(name);
        if (value == null) return def;
        return value.equalsIgnoreCase("true") || value.equals("1") || value.equalsIgnoreCase("on");
    }
}
