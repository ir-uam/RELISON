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
import es.uam.eps.ir.relison.graph.generator.EmptyGraphGenerator;
import es.uam.eps.ir.relison.graph.generator.EmptyMultiGraphGenerator;
import es.uam.eps.ir.relison.graph.generator.GraphGenerator;
import es.uam.eps.ir.relison.sna.community.Communities;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Saving and restoring a whole working session as a single {@code .relison} file.
 *
 * <p>The file is a JSON document with two halves. The <em>server</em> half is everything the backend owns and must
 * rebuild — the network with its type flags and attributes, the detected community partitions, the computed
 * recommendations, the diffusion information pieces and the evaluation test set. The <em>client</em> half is an opaque
 * blob the frontend hands over and gets back untouched: node positions, appearance settings and the computed metric
 * values it displays.</p>
 *
 * <p>The raw diffusion simulation is deliberately <em>not</em> stored: it is streamed to a temporary file precisely
 * because it does not fit comfortably in memory, so a session keeps each run's metric series (enough to redraw the
 * plots) but time-scrubbing a reloaded run needs the simulation to be run again.</p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class SessionController
{
    /** Format version, so a later change can still recognise (or reject) an older file. */
    private static final int VERSION = 1;

    private final GraphStore store;

    public SessionController(GraphStore store)
    {
        this.store = store;
    }

    /**
     * Handles {@code POST /api/session/save}: assembles the session document and returns it for download.
     * @param ctx the request context, with body {@code {graphId, client}}.
     */
    public void save(Context ctx)
    {
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        Object graphId = body.get("graphId");
        GraphSession session = graphId == null ? null : store.get(String.valueOf(graphId));
        if (session == null)
        {
            ctx.status(404).json(Map.of("error", "Unknown graph id."));
            return;
        }

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("relison", VERSION);
        doc.put("created", java.time.Instant.now().toString());
        doc.put("graph", GraphSerializer.toGraphology(session));
        doc.put("schema", GraphSerializer.schema(session));
        doc.put("options", typeFlags(session));
        doc.put("communities", saveCommunities(session));
        doc.put("recommendations", saveRecommendations(session));
        doc.put("pieces", session.getDiffusionPieces());
        doc.put("realPropagated", session.getRealPropagated());
        doc.put("testGraph", saveTestGraph(session));
        doc.put("client", body.get("client"));   // returned verbatim on load
        ctx.json(doc);
    }

    /**
     * Handles {@code POST /api/session/load}: the contents of a {@code .relison} file, posted as the JSON request
     * body (the file is already JSON, so the client sends it straight through rather than as a multipart upload).
     * Rebuilds the network and everything the backend owns as a fresh session, and returns it together with the
     * untouched client blob.
     * @param ctx the request context, whose body is the session document.
     */
    @SuppressWarnings("unchecked")
    public void load(Context ctx)
    {
        Map<?, ?> doc;
        try
        {
            doc = ctx.bodyAsClass(Map.class);
        }
        catch (Exception e)
        {
            ctx.status(400).json(Map.of("error", "Could not read the session file: " + e.getMessage()));
            return;
        }
        if (doc == null || doc.get("relison") == null)
        {
            ctx.status(400).json(Map.of("error", "This is not a RELISON session file."));
            return;
        }
        if (intOf(doc.get("relison"), 0) > VERSION)
        {
            ctx.status(400).json(Map.of("error", "This session was saved by a newer version of RELISON."));
            return;
        }

        try
        {
            Map<?, ?> options = asMap(doc.get("options"));
            boolean directed = boolOf(options.get("directed"), true);
            boolean weighted = boolOf(options.get("weighted"), false);
            boolean multigraph = boolOf(options.get("multigraph"), false);
            boolean selfloops = boolOf(options.get("selfloops"), false);

            Graph<String> graph = rebuildGraph(doc, directed, weighted, multigraph);

            String id = store.newId();
            GraphSession session = new GraphSession(id, graph, directed, weighted, multigraph, selfloops);
            restoreCommunities(session, doc.get("communities"));
            restoreRecommendations(session, doc.get("recommendations"));
            if (doc.get("pieces") instanceof List) session.setDiffusionPieces((List<?>) doc.get("pieces"));
            if (doc.get("realPropagated") instanceof List) session.setRealPropagated((List<?>) doc.get("realPropagated"));
            restoreTestGraph(session, doc.get("testGraph"), directed, multigraph);
            store.register(session);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("graphId", id);
            response.put("stats", GraphSerializer.stats(session));
            response.put("graph", GraphSerializer.toGraphology(session));
            response.put("schema", GraphSerializer.schema(session));
            response.put("client", doc.get("client"));
            ctx.json(response);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            ctx.status(400).json(Map.of("error", "Could not restore the session: " + e));
        }
    }

    /* ------------------------------ save helpers ------------------------------ */

    private Map<String, Object> typeFlags(GraphSession session)
    {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("directed", session.isDirected());
        out.put("weighted", session.isWeighted());
        out.put("multigraph", session.isMultigraph());
        out.put("selfloops", session.allowsSelfLoops());
        return out;
    }

    /** Each partition as a plain node → community-index map. */
    private Map<String, Object> saveCommunities(GraphSession session)
    {
        Map<String, Object> out = new LinkedHashMap<>();
        session.getCommunities().forEach((key, comms) ->
        {
            Map<String, Object> assignment = new LinkedHashMap<>();
            session.getGraph().getAllNodes().forEach(node -> assignment.put(node, comms.getCommunity(node)));
            out.put(key, assignment);
        });
        return out;
    }

    private Map<String, Object> saveRecommendations(GraphSession session)
    {
        Map<String, Object> out = new LinkedHashMap<>();
        session.getRecommendations().forEach((key, result) ->
        {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("label", result.label);
            entry.put("mode", result.mode);
            entry.put("cutoff", result.cutoff);
            List<Map<String, Object>> edges = new ArrayList<>();
            for (RecommendationResult.RecEdge e : result.edges)
            {
                Map<String, Object> edge = new LinkedHashMap<>();
                edge.put("source", e.source);
                edge.put("target", e.target);
                edge.put("score", e.score);
                edges.add(edge);
            }
            entry.put("edges", edges);
            out.put(key, entry);
        });
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("models", out);
        wrapper.put("active", session.getActiveRecommendationKey());
        return wrapper;
    }

    /** The evaluation test set as a bare edge list (its structure is all the evaluation needs). */
    private Object saveTestGraph(GraphSession session)
    {
        Graph<String> test = session.getTestGraph();
        if (test == null) return null;
        List<Map<String, Object>> edges = new ArrayList<>();
        test.getAllNodes().forEach(u -> test.getAdjacentNodes(u).forEach(v ->
        {
            Map<String, Object> edge = new LinkedHashMap<>();
            edge.put("source", u);
            edge.put("target", v);
            edges.add(edge);
        }));
        return edges;
    }

    /* ------------------------------ load helpers ------------------------------ */

    /** Rebuilds the network from the serialized graphology payload, including its declared attributes. */
    private Graph<String> rebuildGraph(Map<?, ?> doc, boolean directed, boolean weighted, boolean multigraph)
            throws Exception
    {
        GraphGenerator<String> ggen = multigraph ? new EmptyMultiGraphGenerator<>() : new EmptyGraphGenerator<>();
        ggen.configure(directed, weighted);
        Graph<String> graph = ggen.generate();

        // Declare the attributes first, so the values below have somewhere to land.
        Map<?, ?> schema = asMap(doc.get("schema"));
        declareAttributes(graph, schema.get("node"), true);
        declareAttributes(graph, schema.get("edge"), false);

        Map<?, ?> g = asMap(doc.get("graph"));
        if (g.get("nodes") instanceof List)
        {
            for (Object o : (List<?>) g.get("nodes"))
            {
                Map<?, ?> node = asMap(o);
                String key = String.valueOf(node.get("key"));
                if (key.isEmpty()) continue;
                graph.addNode(key);
                applyAttributes(graph, key, null, asMap(node.get("attributes")).get("attrs"), true);
            }
        }
        if (g.get("edges") instanceof List)
        {
            for (Object o : (List<?>) g.get("edges"))
            {
                Map<?, ?> edge = asMap(o);
                String source = String.valueOf(edge.get("source"));
                String target = String.valueOf(edge.get("target"));
                if (source.isEmpty() || target.isEmpty()) continue;
                Map<?, ?> attributes = asMap(edge.get("attributes"));
                double weight = doubleOf(attributes.get("weight"), 1.0);
                graph.addNode(source);
                graph.addNode(target);
                graph.addEdge(source, target, weight);
                applyAttributes(graph, source, target, attributes.get("attrs"), false);
            }
        }
        return graph;
    }

    private void declareAttributes(Graph<String> graph, Object defs, boolean node)
    {
        if (!(defs instanceof List)) return;
        for (Object o : (List<?>) defs)
        {
            Map<?, ?> def = asMap(o);
            String name = def.get("name") == null ? null : String.valueOf(def.get("name"));
            if (name == null || name.isEmpty()) continue;
            AttributeType type = AttributeType.fromToken(String.valueOf(def.get("type")));
            if (type == null) type = AttributeType.STRING;
            if (node) graph.defineNodeAttribute(name, type);
            else graph.defineEdgeAttribute(name, type);
        }
    }

    /** Writes back the stored attribute values; they were serialized already typed, so they are set as they are. */
    private void applyAttributes(Graph<String> graph, String a, String b, Object attrs, boolean node)
    {
        if (!(attrs instanceof Map)) return;
        ((Map<?, ?>) attrs).forEach((name, value) ->
        {
            if (name == null || value == null) return;
            try
            {
                if (node) graph.setNodeAttribute(a, String.valueOf(name), value);
                else graph.setEdgeAttribute(a, b, String.valueOf(name), value);
            }
            catch (Exception ignored) { /* an attribute the graph no longer declares is skipped */ }
        });
    }

    private void restoreCommunities(GraphSession session, Object saved)
    {
        if (!(saved instanceof Map)) return;
        ((Map<?, ?>) saved).forEach((key, value) ->
        {
            if (!(value instanceof Map)) return;
            Map<?, ?> assignment = (Map<?, ?>) value;
            // Communities are indexed densely from 0, so create as many as the highest index seen.
            int max = -1;
            for (Object v : assignment.values()) max = Math.max(max, intOf(v, 0));
            Communities<String> comms = new Communities<>();
            for (int i = 0; i <= max; ++i) comms.addCommunity();
            assignment.forEach((node, comm) -> comms.add(String.valueOf(node), intOf(comm, 0)));
            session.getCommunities().put(String.valueOf(key), comms);
        });
    }

    private void restoreRecommendations(GraphSession session, Object saved)
    {
        if (!(saved instanceof Map)) return;
        Map<?, ?> wrapper = (Map<?, ?>) saved;
        Object models = wrapper.get("models");
        if (models instanceof Map)
        {
            ((Map<?, ?>) models).forEach((key, value) ->
            {
                Map<?, ?> entry = asMap(value);
                List<RecommendationResult.RecEdge> edges = new ArrayList<>();
                if (entry.get("edges") instanceof List)
                {
                    for (Object o : (List<?>) entry.get("edges"))
                    {
                        Map<?, ?> e = asMap(o);
                        edges.add(new RecommendationResult.RecEdge(String.valueOf(e.get("source")),
                                String.valueOf(e.get("target")), doubleOf(e.get("score"), 0.0)));
                    }
                }
                session.getRecommendations().put(String.valueOf(key), new RecommendationResult(
                        String.valueOf(entry.get("label")), String.valueOf(entry.get("mode")),
                        intOf(entry.get("cutoff"), 10), edges));
            });
        }
        Object active = wrapper.get("active");
        if (active != null && session.getRecommendations().containsKey(String.valueOf(active)))
        {
            session.setActiveRecommendation(String.valueOf(active));
        }
    }

    private void restoreTestGraph(GraphSession session, Object saved, boolean directed, boolean multigraph)
            throws Exception
    {
        if (!(saved instanceof List)) return;
        GraphGenerator<String> ggen = multigraph ? new EmptyMultiGraphGenerator<>() : new EmptyGraphGenerator<>();
        ggen.configure(directed, false);
        Graph<String> test = ggen.generate();
        for (Object o : (List<?>) saved)
        {
            Map<?, ?> e = asMap(o);
            String source = String.valueOf(e.get("source"));
            String target = String.valueOf(e.get("target"));
            if (source.isEmpty() || target.isEmpty()) continue;
            test.addNode(source);
            test.addNode(target);
            test.addEdge(source, target);
        }
        session.setTestGraph(test);
    }

    /* ------------------------------ small helpers ------------------------------ */

    private static Map<?, ?> asMap(Object o) { return o instanceof Map ? (Map<?, ?>) o : Map.of(); }

    private static boolean boolOf(Object o, boolean def)
    {
        if (o instanceof Boolean) return (Boolean) o;
        if (o == null) return def;
        return Boolean.parseBoolean(String.valueOf(o));
    }

    private static int intOf(Object o, int def)
    {
        if (o instanceof Number) return ((Number) o).intValue();
        if (o == null) return def;
        try { return Integer.parseInt(String.valueOf(o).trim()); }
        catch (NumberFormatException e) { return def; }
    }

    private static double doubleOf(Object o, double def)
    {
        if (o instanceof Number) return ((Number) o).doubleValue();
        if (o == null) return def;
        try { return Double.parseDouble(String.valueOf(o).trim()); }
        catch (NumberFormatException e) { return def; }
    }
}
