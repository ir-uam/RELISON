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
import es.uam.eps.ir.relison.graph.generator.CompleteGraphGenerator;
import es.uam.eps.ir.relison.graph.generator.GraphGenerator;
import es.uam.eps.ir.relison.graph.generator.NoLinksGraphGenerator;
import es.uam.eps.ir.relison.graph.generator.random.BarabasiGenerator;
import es.uam.eps.ir.relison.graph.generator.random.ErdosGenerator;
import es.uam.eps.ir.relison.graph.generator.random.WattsStrogatzGenerator;
import es.uam.eps.ir.relison.io.graph.GexfGraphReader;
import es.uam.eps.ir.relison.io.graph.GraphReader;
import es.uam.eps.ir.relison.io.graph.PajekGraphReader;
import es.uam.eps.ir.relison.io.graph.TextGraphReader;
import es.uam.eps.ir.relison.io.graph.TextMultiGraphReader;
import es.uam.eps.ir.relison.utils.generator.Generator;
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

        // The network file format: an edge list by default, or Pajek / GEXF. The client sends it explicitly, but fall
        // back to the file extension so a direct API call does the sensible thing too.
        String format = ctx.formParam("format");
        if (format == null || format.isBlank()) format = formatFromName(file.filename());

        GraphReader<String> reader = switch (format)
        {
            case "pajek" -> new PajekGraphReader<>(multigraph, directed, weighted, selfloops, Parsers.sp);
            case "gexf" -> new GexfGraphReader<>(multigraph, directed, weighted, selfloops, Parsers.sp);
            default -> multigraph
                    ? new TextMultiGraphReader<>(directed, weighted, selfloops, "\t", Parsers.sp)
                    : new TextGraphReader<>(directed, weighted, selfloops, "\t", Parsers.sp);
        };

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
     * Handles {@code POST /api/graph/generate}: creates a synthetic network with one of RELISON's graph generators
     * and registers it as a session, returning the same payload as {@code load}. Body:
     * {@code {type: "erdos"|"barabasi"|"watts"|"complete"|"empty", directed, params: {…}}} where {@code params}
     * holds the model-specific settings (see each case below). Generated graphs are unweighted simple graphs.
     * @param ctx the request context.
     */
    public void generate(Context ctx)
    {
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        String type = body.get("type") == null ? "" : body.get("type").toString();
        boolean directed = Boolean.TRUE.equals(body.get("directed"));
        Map<?, ?> params = (body.get("params") instanceof Map) ? (Map<?, ?>) body.get("params") : Map.of();

        // A fresh sequential node-id generator ("0", "1", …) per request: the shared Generators.stringgen instance
        // keeps state between calls, so reusing it would continue numbering across generations (and race).
        Generator<String> nodeGen = new Generator<>()
        {
            private int prev = -1;

            @Override
            public String generate() { return String.valueOf(++prev); }

            @Override
            public void reset() { prev = -1; }

            @Override
            public void reset(String val) { prev = Integer.parseInt(val); }
        };

        GraphGenerator<String> gen;
        switch (type)
        {
            case "erdos":
            {
                int nodes = Math.max(1, intOf(params, "nodes", 100));
                double prob = Math.min(1.0, Math.max(0.0, doubleOf(params, "prob", 0.05)));
                ErdosGenerator<String> g = new ErdosGenerator<>();
                g.configure(directed, nodes, prob, nodeGen);
                gen = g;
                break;
            }
            case "barabasi":
            {
                int initial = Math.max(1, intOf(params, "initialNodes", 5));
                int iter = Math.max(1, intOf(params, "numIter", 95));
                int edgesIter = Math.max(1, intOf(params, "numEdgesIter", 2));
                if (edgesIter > initial)
                {
                    ctx.status(400).json(Map.of("error", "Barabási-Albert: edges per iteration must not exceed the number of initial nodes."));
                    return;
                }
                BarabasiGenerator<String> g = new BarabasiGenerator<>();
                g.configure(directed, initial, iter, edgesIter, nodeGen);
                gen = g;
                break;
            }
            case "watts":
            {
                int nodes = Math.max(3, intOf(params, "nodes", 100));
                int degree = Math.max(2, intOf(params, "meanDegree", 4));
                double beta = Math.min(1.0, Math.max(0.0, doubleOf(params, "beta", 0.1)));
                WattsStrogatzGenerator<String> g = new WattsStrogatzGenerator<>();
                g.configure(directed, nodes, degree, beta, nodeGen);
                gen = g;
                break;
            }
            case "complete":
            {
                int nodes = Math.max(1, intOf(params, "nodes", 20));
                CompleteGraphGenerator<String> g = new CompleteGraphGenerator<>();
                g.configure(directed, nodes, nodeGen);
                gen = g;
                break;
            }
            case "empty":
            {
                int nodes = Math.max(1, intOf(params, "nodes", 50));
                NoLinksGraphGenerator<String> g = new NoLinksGraphGenerator<>();
                g.configure(directed, nodes, nodeGen);
                gen = g;
                break;
            }
            default:
                ctx.status(400).json(Map.of("error", "Unknown generator type: '" + type + "'."));
                return;
        }

        Graph<String> graph;
        try
        {
            graph = gen.generate();
        }
        catch (Exception e)
        {
            ctx.status(400).json(Map.of("error", "Could not generate the graph: " + e.getMessage()));
            return;
        }

        String id = store.newId();
        GraphSession session = new GraphSession(id, graph, directed, false, false, false);
        store.register(session);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("graphId", id);
        response.put("stats", GraphSerializer.stats(session));
        response.put("graph", GraphSerializer.toGraphology(session));
        response.put("schema", GraphSerializer.schema(session));
        ctx.json(response);
    }

    /** Reads an integer from a JSON params map, falling back to a default. */
    private static int intOf(Map<?, ?> params, String name, int def)
    {
        Object v = params.get(name);
        if (v instanceof Number) return ((Number) v).intValue();
        if (v == null) return def;
        try { return (int) Double.parseDouble(v.toString().trim()); }
        catch (NumberFormatException e) { return def; }
    }

    /** Reads a double from a JSON params map, falling back to a default. */
    private static double doubleOf(Map<?, ?> params, String name, double def)
    {
        Object v = params.get(name);
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v == null) return def;
        try { return Double.parseDouble(v.toString().trim()); }
        catch (NumberFormatException e) { return def; }
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
    /**
     * Infers the network file format from a file name, so an upload without an explicit {@code format} field is still
     * read correctly.
     * @param name the uploaded file name (may be {@code null}).
     * @return {@code "pajek"}, {@code "gexf"}, or {@code "edges"} for anything else.
     */
    private static String formatFromName(String name)
    {
        if (name == null) return "edges";
        String lower = name.toLowerCase();
        if (lower.endsWith(".net") || lower.endsWith(".paj")) return "pajek";
        if (lower.endsWith(".gexf")) return "gexf";
        return "edges";
    }

    static boolean boolParam(Context ctx, String name, boolean def)
    {
        String value = ctx.formParam(name);
        if (value == null) return def;
        return value.equalsIgnoreCase("true") || value.equals("1") || value.equalsIgnoreCase("on");
    }
}
