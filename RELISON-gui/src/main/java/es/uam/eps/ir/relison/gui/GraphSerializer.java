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
import es.uam.eps.ir.relison.graph.attributes.AttributeType;
import es.uam.eps.ir.relison.graph.multigraph.MultiGraph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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

        // Names of the user-defined node/edge attributes (collected once and reused per node/edge).
        List<String> nodeAttrNames = new ArrayList<>();
        graph.getNodeAttributeNames().forEach(nodeAttrNames::add);
        List<String> edgeAttrNames = new ArrayList<>();
        graph.getEdgeAttributeNames().forEach(edgeAttrNames::add);

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
            // User-defined attributes are nested under "attrs" so they cannot clash with sigma's reserved keys.
            attributes.put("attrs", nodeAttrValues(graph, node, nodeAttrNames));

            Map<String, Object> nodeObj = new LinkedHashMap<>();
            nodeObj.put("key", node);
            nodeObj.put("attributes", attributes);
            nodes.add(nodeObj);
        }
        result.put("nodes", nodes);

        // Edges: iterate the adjacency (outgoing) of each node. For undirected graphs each edge is reported
        // from both endpoints, so we keep it only once (when source <= target).
        List<Map<String, Object>> edges = new ArrayList<>();
        if (session.isMultigraph())
        {
            // Emit each parallel edge separately, with its own weight, attributes and stable id as the edge key,
            // so the frontend can tell parallel edges apart.
            MultiGraph<String> mg = (MultiGraph<String>) graph;
            for (String source : nodeList)
            {
                List<String> targets = new ArrayList<>();
                mg.getAdjacentNodes(source).forEach(targets::add);
                for (String target : targets)
                {
                    if (!directed && source.compareTo(target) > 0) continue;
                    List<Double> ws = mg.getEdgeWeights(source, target);
                    List<Long> ids = mg.getEdgeIds(source, target);
                    for (int k = 0; k < ws.size(); ++k)
                    {
                        Map<String, Object> attributes = new LinkedHashMap<>();
                        attributes.put("weight", ws.get(k));
                        attributes.put("attrs", edgeAttrValuesAt(mg, source, target, k, edgeAttrNames));

                        Map<String, Object> edgeObj = new LinkedHashMap<>();
                        if (k < ids.size()) edgeObj.put("key", Long.toString(ids.get(k)));
                        edgeObj.put("source", source);
                        edgeObj.put("target", target);
                        edgeObj.put("attributes", attributes);
                        edges.add(edgeObj);
                    }
                }
            }
        }
        else
        {
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
                    attributes.put("attrs", edgeAttrValues(graph, source, target, edgeAttrNames));

                    Map<String, Object> edgeObj = new LinkedHashMap<>();
                    edgeObj.put("source", source);
                    edgeObj.put("target", target);
                    edgeObj.put("attributes", attributes);
                    edges.add(edgeObj);
                }
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

    /**
     * Describes the node and edge attribute schemas of a session, so the frontend can build columns and menus.
     * @param session the session.
     * @return a map {@code {node:[{name,type,numeric}], edge:[{name,type,numeric}]}}.
     */
    public static Map<String, Object> schema(GraphSession session)
    {
        Graph<String> graph = session.getGraph();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("node", attrDefs(graph.getNodeAttributeNames(), graph::getNodeAttributeType));
        out.put("edge", attrDefs(graph.getEdgeAttributeNames(), graph::getEdgeAttributeType));
        return out;
    }

    /** Builds the list of attribute definitions ({name,type,numeric}) for a stream of attribute names. */
    private static List<Map<String, Object>> attrDefs(java.util.stream.Stream<String> names, Function<String, AttributeType> typeOf)
    {
        List<Map<String, Object>> list = new ArrayList<>();
        names.forEach(name ->
        {
            AttributeType type = typeOf.apply(name);
            Map<String, Object> def = new LinkedHashMap<>();
            def.put("name", name);
            def.put("type", type == null ? "string" : type.token());
            def.put("numeric", type != null && type.isNumeric());
            list.add(def);
        });
        return list;
    }

    /** Collects the non-null attribute values of a node into a name-&gt;value map. */
    static Map<String, Object> nodeAttrValues(Graph<String> graph, String node, List<String> names)
    {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String name : names)
        {
            Object value = graph.getNodeAttribute(node, name);
            if (value != null) map.put(name, value);
        }
        return map;
    }

    /** Collects the non-null attribute values of an edge into a name-&gt;value map. */
    static Map<String, Object> edgeAttrValues(Graph<String> graph, String source, String target, List<String> names)
    {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String name : names)
        {
            Object value = graph.getEdgeAttribute(source, target, name);
            if (value != null) map.put(name, value);
        }
        return map;
    }

    /** Collects the non-null attribute values of a specific parallel edge into a name-&gt;value map. */
    static Map<String, Object> edgeAttrValuesAt(MultiGraph<String> graph, String source, String target, int idx, List<String> names)
    {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String name : names)
        {
            Object value = graph.getEdgeAttribute(source, target, idx, name);
            if (value != null) map.put(name, value);
        }
        return map;
    }
}
