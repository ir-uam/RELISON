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
import io.javalin.http.Context;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST handler that enumerates all shortest (minimum-hop) paths between two nodes.
 *
 * <p>RELISON's distance calculator only reports the <em>number</em> of geodesics, so the paths themselves are
 * recovered here with a breadth-first search that records, for each node, the set of predecessors lying on a
 * shortest path from the source. Backtracking that predecessor DAG yields every shortest path.</p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class PathController
{
    private final GraphStore store;
    /** Maximum number of paths returned, to avoid combinatorial blow-up. */
    private static final int MAX_PATHS = 2000;

    public PathController(GraphStore store)
    {
        this.store = store;
    }

    /**
     * Handles {@code POST /api/paths}: returns all shortest paths between two nodes.
     * @param ctx the request context, with body {@code {graphId, source, target, allowedNodes?, allowedLinks?}}.
     */
    public void shortestPaths(Context ctx)
    {
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        Object graphId = body.get("graphId");
        GraphSession session = graphId == null ? null : store.get(String.valueOf(graphId));
        if (session == null)
        {
            ctx.status(404).json(Map.of("error", "Unknown graph id."));
            return;
        }

        String source = parse(ctx, body.get("source"));
        String target = parse(ctx, body.get("target"));
        if (source == null || target == null) return;

        // Optionally search over the graph augmented with the active recommendation's links.
        boolean withRec = MetricController.useRecommendation(body, session);
        Graph<String> graph = withRec ? session.getAugmentedGraph() : session.getGraph();
        if (!graph.containsVertex(source) || !graph.containsVertex(target))
        {
            ctx.status(400).json(Map.of("error", "Both nodes must exist in the graph."));
            return;
        }

        try
        {
            Map<String, Object> response = computePaths(graph, source, target, body);
            response.put("recommendation", withRec ? session.getActiveRecommendationKey() : null);
            ctx.json(response);
        }
        catch (IllegalArgumentException exception)
        {
            ctx.status(400).json(Map.of("error", exception.getMessage()));
        }
    }

    /** Search the supplied working subgraph, without modifying the session graph. */
    static Map<String, Object> computePaths(Graph<String> graph, String source, String target, Map<?, ?> body)
    {
        if (!graph.containsVertex(source) || !graph.containsVertex(target)) throw new IllegalArgumentException("Both nodes must exist in the graph.");
        Set<String> allowedNodes = null;
        if (body.containsKey("allowedNodes"))
        {
            if (!(body.get("allowedNodes") instanceof List)) throw new IllegalArgumentException("allowedNodes must be a list of node IDs.");
            allowedNodes = new HashSet<>();
            for (Object node : (List<?>) body.get("allowedNodes"))
            {
                if (!(node instanceof String) || !graph.containsVertex((String) node)) throw new IllegalArgumentException("Every allowed node must exist in the graph.");
                allowedNodes.add((String) node);
            }
            if (!allowedNodes.contains(source) || !allowedNodes.contains(target)) throw new IllegalArgumentException("Both endpoints must belong to the filtered graph.");
        }
        Set<List<String>> allowedLinks = null;
        if (body.containsKey("allowedLinks"))
        {
            if (!(body.get("allowedLinks") instanceof List)) throw new IllegalArgumentException("allowedLinks must be a list of endpoint pairs.");
            allowedLinks = new HashSet<>();
            for (Object item : (List<?>) body.get("allowedLinks"))
            {
                if (!(item instanceof List)) throw new IllegalArgumentException("Every allowed link must be an endpoint pair.");
                List<?> link = (List<?>) item;
                if (link.size() != 2 || !(link.get(0) instanceof String) || !(link.get(1) instanceof String)) throw new IllegalArgumentException("Every allowed link must contain two node IDs.");
                String a = (String) link.get(0), b = (String) link.get(1);
                if (!graph.containsEdge(a, b)) throw new IllegalArgumentException("Every allowed link must exist in the graph.");
                allowedLinks.add(List.of(a, b));
                if (!graph.isDirected()) allowedLinks.add(List.of(b, a));
            }
        }
        Map<String, Integer> dist = new HashMap<>();
        Map<String, List<String>> preds = new HashMap<>();
        dist.put(source, 0);
        Deque<String> queue = new ArrayDeque<>();
        queue.add(source);
        while (!queue.isEmpty())
        {
            String u = queue.poll();
            int du = dist.get(u);
            List<String> successors = new ArrayList<>();
            graph.getAdjacentNodes(u).forEach(successors::add);
            for (String w : successors)
            {
                if (allowedNodes != null && !allowedNodes.contains(w) || allowedLinks != null && !allowedLinks.contains(List.of(u, w))) continue;
                Integer dw = dist.get(w);
                if (dw == null)
                {
                    dist.put(w, du + 1);
                    List<String> predecessors = new ArrayList<>();
                    predecessors.add(u); preds.put(w, predecessors); queue.add(w);
                }
                else if (dw == du + 1 && !preds.get(w).contains(u)) preds.get(w).add(u);
            }
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("source", source); response.put("target", target);
        Integer length = dist.get(target);
        List<List<String>> paths = new ArrayList<>();
        boolean truncated = length != null && backtrack(target, source, preds, new ArrayDeque<>(), paths);
        response.put("length", length == null ? -1 : length);
        response.put("count", paths.size()); response.put("truncated", truncated); response.put("paths", paths);
        return response;
    }

    /**
     * Recursively builds shortest paths by following predecessors from {@code node} back to {@code source}.
     * @return true if enumeration was stopped early because the path cap was reached.
     */
    private static boolean backtrack(String node, String source, Map<String, List<String>> preds, Deque<String> suffix, List<List<String>> out)
    {
        if (out.size() >= MAX_PATHS) return true;
        suffix.addFirst(node);
        boolean truncated = false;
        if (node.equals(source))
        {
            out.add(new ArrayList<>(suffix));
        }
        else
        {
            for (String p : preds.getOrDefault(node, List.of()))
            {
                if (backtrack(p, source, preds, suffix, out)) { truncated = true; break; }
            }
        }
        suffix.removeFirst();
        return truncated;
    }

    private String parse(Context ctx, Object raw)
    {
        if (raw == null)
        {
            ctx.status(400).json(Map.of("error", "Both source and target are required."));
            return null;
        }
        // Node identifiers are arbitrary strings.
        String id = String.valueOf(raw).trim();
        if (id.isEmpty())
        {
            ctx.status(400).json(Map.of("error", "Both source and target are required."));
            return null;
        }
        return id;
    }
}
