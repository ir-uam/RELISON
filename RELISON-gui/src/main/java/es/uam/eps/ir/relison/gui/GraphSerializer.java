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
import es.uam.eps.ir.relison.graph.Weight;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts a RELISON {@link Graph} into the
 * <a href="https://graphology.github.io/serialization.html">graphology serialized format</a>, which sigma.js
 * can import directly via {@code graph.import(data)}.
 *
 * <p>The produced structure is:</p>
 * <pre>
 * {
 *   "options": { "type": "directed|undirected", "multi": false, "allowSelfLoops": true },
 *   "nodes":   [ { "key": "0", "attributes": { "label": "0", "x": .., "y": .., "size": 4 } }, ... ],
 *   "edges":   [ { "source": "0", "target": "1", "attributes": { "weight": 1.0 } }, ... ]
 * }
 * </pre>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public final class GraphSerializer
{
    private GraphSerializer()
    {
    }

    /**
     * Serializes the network held by a session into the graphology format.
     * @param session the session whose graph is serialized.
     * @return a JSON-serializable map.
     */
    public static Map<String, Object> toGraphology(GraphSession session)
    {
        Graph<String> graph = session.getGraph();
        boolean directed = session.isDirected();

        Map<String, Object> result = new LinkedHashMap<>();

        Map<String, Object> options = new LinkedHashMap<>();
        options.put("type", directed ? "directed" : "undirected");
        options.put("multi", session.isMultigraph());
        options.put("allowSelfLoops", session.allowsSelfLoops());
        result.put("options", options);

        // Nodes: lay them out on a circle as an initial position; ForceAtlas2 refines this on the client.
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<String> nodeList = new ArrayList<>();
        graph.getAllNodes().forEach(nodeList::add);
        int n = nodeList.size();
        for (int i = 0; i < n; ++i)
        {
            String node = nodeList.get(i);
            double angle = (2.0 * Math.PI * i) / Math.max(1, n);
            Map<String, Object> attributes = new LinkedHashMap<>();
            attributes.put("label", node);
            attributes.put("x", Math.cos(angle) * 100.0);
            attributes.put("y", Math.sin(angle) * 100.0);
            attributes.put("size", 4);

            Map<String, Object> nodeObj = new LinkedHashMap<>();
            nodeObj.put("key", node);
            nodeObj.put("attributes", attributes);
            nodes.add(nodeObj);
        }
        result.put("nodes", nodes);

        // Edges: iterate the adjacency (outgoing) of each node. For undirected graphs each edge is reported
        // from both endpoints, so we keep it only once (when source <= target).
        List<Map<String, Object>> edges = new ArrayList<>();
        for (String source : nodeList)
        {
            List<Weight<String, Double>> adjacent = new ArrayList<>();
            graph.getAdjacentNodesWeights(source).forEach(adjacent::add);
            for (Weight<String, Double> w : adjacent)
            {
                String target = w.getIdx();
                if (!directed && source.compareTo(target) > 0) continue;

                Map<String, Object> attributes = new LinkedHashMap<>();
                attributes.put("weight", w.getValue());

                Map<String, Object> edgeObj = new LinkedHashMap<>();
                edgeObj.put("source", source);
                edgeObj.put("target", target);
                edgeObj.put("attributes", attributes);
                edges.add(edgeObj);
            }
        }
        result.put("edges", edges);

        return result;
    }

    /**
     * Builds the small statistics object describing the size of the network.
     * @param session the session.
     * @return a JSON-serializable map with the node and edge counts.
     */
    public static Map<String, Object> stats(GraphSession session)
    {
        Graph<String> graph = session.getGraph();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("nodes", graph.getVertexCount());
        stats.put("edges", graph.getEdgeCount());
        stats.put("directed", session.isDirected());
        stats.put("weighted", session.isWeighted());
        stats.put("multigraph", session.isMultigraph());
        return stats;
    }
}
