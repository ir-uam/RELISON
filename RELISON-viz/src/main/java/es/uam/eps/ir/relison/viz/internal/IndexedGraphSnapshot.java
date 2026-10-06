/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.internal;


import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.multigraph.MultiGraph;
import es.uam.eps.ir.relison.viz.LayoutRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Immutable indexed topology and weights. Outgoing arcs preserve parallel edges
 * and self-loops; undirected edges appear in each endpoint's outgoing arcs.
 * This is an internal representation, not a replacement for RELISON Graph.
 * @param <U> node type
 */
public final class IndexedGraphSnapshot<U>
{
    private final List<U> nodes;
    private final Map<U, Integer> indices;
    private final int[][] targets;
    private final double[][] weights;
    private final boolean directed;

    private IndexedGraphSnapshot(Graph<U> graph, LayoutRequest<U> request)
    {
        List<U> encountered = graph.getAllNodes().collect(Collectors.toList());
        Set<U> unique = new HashSet<>(encountered);
        if (unique.contains(null) || unique.size() != encountered.size())
            throw new IllegalArgumentException("Graph nodes must be non-null and unique");
        List<U> ordered = request.getNodeOrder().orElse(encountered);
        if (ordered.size() != unique.size() || !unique.equals(new HashSet<>(ordered)))
            throw new IllegalArgumentException("Node order must contain exactly the graph nodes");
        if (!unique.containsAll(request.getInitialPositions().keySet()))
            throw new IllegalArgumentException("Initial positions refer to nodes outside the graph");
        nodes = Collections.unmodifiableList(new ArrayList<>(ordered));
        indices = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) indices.put(nodes.get(i), i);
        directed = graph.isDirected();
        targets = new int[nodes.size()][];
        weights = new double[nodes.size()][];
        for (int i = 0; i < nodes.size(); i++)
        {
            U source = nodes.get(i);
            List<Integer> adjacent = graph.getAdjacentNodes(source).distinct().map(node -> {
                Integer index = indices.get(node);
                if (index == null) throw new IllegalArgumentException("Graph changed during snapshot capture");
                return index;
            }).sorted().collect(Collectors.toList());
            List<Integer> targetList = new ArrayList<>();
            List<Double> weightList = new ArrayList<>();
            for (int target : adjacent)
            {
                U node = nodes.get(target);
                List<Double> edgeWeights;
                if (graph instanceof MultiGraph)
                    edgeWeights = ((MultiGraph<U>) graph).getEdgeWeights(source, node);
                else edgeWeights = Collections.singletonList(graph.getEdgeWeight(source, node));
                for (double weight : edgeWeights)
                {
                    targetList.add(target);
                    weightList.add(weight);
                }
            }
            targets[i] = targetList.stream().mapToInt(Integer::intValue).toArray();
            weights[i] = weightList.stream().mapToDouble(Double::doubleValue).toArray();
        }
    }

    /**
     * @param graph graph to snapshot
     * @param request ordering and constraints
     * @param <U> node type
     * @return snapshot
     */
    public static <U> IndexedGraphSnapshot<U> capture(Graph<U> graph, LayoutRequest<U> request)
    {
        return new IndexedGraphSnapshot<>(Objects.requireNonNull(graph), Objects.requireNonNull(request));
    }
    /** @return ordered immutable node list */
    public List<U> getNodes() { return nodes; }
    /**
     * @param node node
     * @return index, or -1 if absent
     */
    public int indexOf(U node) { return indices.getOrDefault(node, -1); }
    /** @return whether arcs are directed */
    public boolean isDirected() { return directed; }
    /**
     * @param index source index
     * @return defensive copy of outgoing target indices
     */
    public int[] getTargets(int index) { return targets[index].clone(); }
    /**
     * @param index source index
     * @return defensive copy of aligned weights
     */
    public double[] getWeights(int index) { return weights[index].clone(); }
}
