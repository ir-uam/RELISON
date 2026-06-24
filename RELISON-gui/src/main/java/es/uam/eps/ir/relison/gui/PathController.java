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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
     * @param ctx the request context, with body {@code {graphId, source, target}}.
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

        Graph<String> graph = session.getGraph();
        if (!graph.containsVertex(source) || !graph.containsVertex(target))
        {
            ctx.status(400).json(Map.of("error", "Both nodes must exist in the graph."));
            return;
        }

        // BFS from the source, recording the shortest-path predecessors of each node.
        Map<String, Integer> dist = new HashMap<>();
        Map<String, List<String>> preds = new HashMap<>();
        dist.put(source, 0);
        Deque<String> queue = new ArrayDeque<>();
        queue.add(source);
        while (!queue.isEmpty())
        {
            String u = queue.poll();
            int du = dist.get(u);
            List<String> succ = new ArrayList<>();
            graph.getAdjacentNodes(u).forEach(succ::add);
            for (String w : succ)
            {
                Integer dw = dist.get(w);
                if (dw == null)
                {
                    dist.put(w, du + 1);
                    List<String> p = new ArrayList<>();
                    p.add(u);
                    preds.put(w, p);
                    queue.add(w);
                }
                else if (dw == du + 1)
                {
                    preds.get(w).add(u);
                }
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("source", source);
        response.put("target", target);

        Integer length = source.equals(target) ? 0 : dist.get(target);
        if (length == null)
        {
            response.put("length", -1);
            response.put("count", 0);
            response.put("truncated", false);
            response.put("paths", List.of());
            ctx.json(response);
            return;
        }

        // Backtrack the predecessor DAG to build every shortest path (capped at MAX_PATHS).
        List<List<String>> paths = new ArrayList<>();
        boolean truncated = backtrack(target, source, preds, new ArrayDeque<>(), paths);

        response.put("length", length);
        response.put("count", paths.size());
        response.put("truncated", truncated);
        response.put("paths", paths);
        ctx.json(response);
    }

    /**
     * Recursively builds shortest paths by following predecessors from {@code node} back to {@code source}.
     * @return true if enumeration was stopped early because the path cap was reached.
     */
    private boolean backtrack(String node, String source, Map<String, List<String>> preds, Deque<String> suffix, List<List<String>> out)
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
