/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.sna.metrics.graph;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.multigraph.DirectedMultiGraph;
import es.uam.eps.ir.relison.graph.multigraph.UndirectedMultiGraph;
import es.uam.eps.ir.relison.sna.metrics.GraphMetric;
import es.uam.eps.ir.relison.utils.indexes.GiniIndex;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes the value for Gini for the different pairs of nodes.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class EdgeGini<U> implements GraphMetric<U>
{

    private final EdgeGiniMode mode;

    /**
     * Constructor.
     *
     * @param mode Execution mode. If the mode is COMPLETE, iterates over each pair
     *             of vertices in the graph. If the mode is SEMICOMPLETE, iterates over each pair,
     *             but autoloops are contained in a single category. If the mode is INTERLINKS, only
     *             links between different edges are applied.
     */
    public EdgeGini(EdgeGiniMode mode)
    {
        this.mode = mode;
    }

    /**
     * Computes the Pair Gini of the nodes (how equally distributed are links between two different nodes)
     *
     * @param graph the graph.
     *
     * @return if it is not a multigraph, then, the value is equals to NaN (it does not make sense, since it depends on the density of the graph).
     */
    @Override
    public double compute(Graph<U> graph)
    {
        if (!graph.isMultigraph())
        {
            return Double.NaN;
        }

        if (graph.isDirected())
        {
            return computeDirected((DirectedMultiGraph<U>) graph);
        }
        return computeUndirected((UndirectedMultiGraph<U>) graph);
    }

    /**
     * Computes the Pair Gini index for the directed graph case.
     *
     * @param graph The directed multigraph.
     *
     * @return The value of the metric.
     */
    private double computeDirected(DirectedMultiGraph<U> graph)
    {

        List<Double> degrees = new ArrayList<>();

        graph.getAllNodes().forEach((orig) -> graph.getAllNodes().forEach((dest) ->
        {
            if (!orig.equals(dest))
            {
                degrees.add(graph.getNumEdges(orig, dest) + 0.0);
            }
        }));


        long sumAutoLoops = graph.getAllNodes().mapToLong(u -> graph.getNumEdges(u, u)).sum();
        if (this.mode.equals(EdgeGiniMode.SEMICOMPLETE))
        {
            degrees.add(sumAutoLoops + 0.0);
        }
        else if (this.mode.equals(EdgeGiniMode.COMPLETE))
        {
            graph.getAllNodes().forEach(u -> degrees.add(graph.getNumEdges(u, u) + 0.0));
        }

        long vertexCount;
        long edgeCount;

        switch (this.mode)
        {
            case COMPLETE ->
            {
                vertexCount = graph.getVertexCount() * graph.getVertexCount();
                edgeCount = graph.getEdgeCount();
            }
            case SEMICOMPLETE ->
            {
                vertexCount = graph.getVertexCount() * (graph.getVertexCount() - 1) + 1;
                edgeCount = graph.getEdgeCount();
            }
            default ->
            {
                vertexCount = graph.getVertexCount() * (graph.getVertexCount() - 1);
                edgeCount = graph.getEdgeCount() - sumAutoLoops;
            }
        }

        GiniIndex gi = new GiniIndex();
        double value = gi.compute(degrees, true, vertexCount, edgeCount);
        return 1.0 - value;
    }

    /**
     * Computes the Pair Gini index for the undirected graph case
     *
     * @param graph The undirected multigraph
     *
     * @return The value of the metric.
     */
    private double computeUndirected(UndirectedMultiGraph<U> graph)
    {

        List<Double> degrees = new ArrayList<>();
        List<U> visited = new ArrayList<>();
        graph.getAllNodes().forEach(orig ->
        {
            graph.getAllNodes().forEach(dest ->
            {
                if (!visited.contains(dest) && !orig.equals(dest))
                {
                    degrees.add(graph.getNumEdges(orig, dest) + 0.0);
                }
            });
            visited.add(orig);
        });

        long sumAutoloops = graph.getAllNodes().mapToLong(u -> graph.getNumEdges(u, u)).sum();
        if (this.mode.equals(EdgeGiniMode.SEMICOMPLETE))
        {
            degrees.add(sumAutoloops + 0.0);
        }
        else if (this.mode.equals(EdgeGiniMode.COMPLETE))
        {
            graph.getAllNodes().forEach(u -> degrees.add(graph.getNumEdges(u, u) + 0.0));
        }

        long vertexCount;
        long edgeCount;

        switch (this.mode)
        {
            case COMPLETE ->
            {
                vertexCount = graph.getVertexCount() * (graph.getVertexCount() + 1) / 2;
                edgeCount = graph.getEdgeCount();
            }
            case SEMICOMPLETE ->
            {
                vertexCount = graph.getVertexCount() * (graph.getVertexCount() - 1) / 2 + 1;
                edgeCount = graph.getEdgeCount();
            }
            default ->
            {
                vertexCount = graph.getVertexCount() * (graph.getVertexCount() - 1) / 2;
                edgeCount = graph.getEdgeCount() - sumAutoloops;
            }
        }

        GiniIndex gi = new GiniIndex();
        double value = gi.compute(degrees, true, vertexCount, edgeCount);
        return 1.0 - value;
    }
}
