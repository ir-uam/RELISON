/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.metrics.complementary.vertex;

import es.uam.eps.ir.sonalire.graph.DirectedGraph;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.graph.UndirectedGraph;
import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;
import es.uam.eps.ir.sonalire.metrics.VertexMetric;

import java.util.HashMap;
import java.util.Map;

/**
 * Computes the degree of a given user in a graph.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class ComplementaryDegree<U> implements VertexMetric<U>
{
    /**
     * The orientation in which the score is computed.
     */
    private final EdgeOrientation orientation;

    /**
     * Constructor.
     *
     * @param orientation The orientation in which to take the value.
     */
    public ComplementaryDegree(EdgeOrientation orientation)
    {
        this.orientation = orientation;
    }

    @Override
    public double compute(Graph<U> graph, U user)
    {
        if (graph.isDirected())
        {
            return this.computeDirected((DirectedGraph<U>) graph, user);
        }
        else // the graph is not directed
        {
            return this.computeUndirected((UndirectedGraph<U>) graph, user);
        }
    }

    @Override
    public Map<U, Double> compute(Graph<U> graph)
    {
        Map<U, Double> metrics = new HashMap<>();
        graph.getAllNodes().forEach((node) -> metrics.put(node, this.compute(graph, node)));
        return metrics;
    }

    @Override
    public double averageValue(Graph<U> graph)
    {
        if (graph.getVertexCount() > 0)
        {
            if (graph.isDirected())
            {
                return (graph.getVertexCount() * (graph.getVertexCount()) - graph.getEdgeCount() + 0.0) / (graph.getVertexCount() + 0.0);
            }
            else
            {
                return 2.0 * (graph.getVertexCount() * (graph.getVertexCount() - 1) / 2.0 + graph.getVertexCount() - graph.getEdgeCount() + 0.0) / (graph.getVertexCount() + 0.0);
            }
        }
        return 0.0;
    }

    /**
     * Computes the degree of the user in a directed graph.
     *
     * @param directedGraph The directed graph to take.
     * @param user          The user.
     *
     * @return the corresponding degree value.
     */
    private double computeDirected(DirectedGraph<U> directedGraph, U user)
    {
        return switch (orientation)
        {
            case IN -> directedGraph.getVertexCount() - directedGraph.inDegree(user);
            case OUT -> directedGraph.getVertexCount() - directedGraph.outDegree(user);
            case MUTUAL -> throw new UnsupportedOperationException("Mutual neighborhood: operation not supported yet");
            default -> 2 * directedGraph.getVertexCount() - directedGraph.degree(user);
        };

    }

    /**
     * Computes the degree of the user in an undirected graph.
     *
     * @param undirectedGraph The undirected graph to take.
     * @param user            The user.
     *
     * @return the degree of the user in that graph.
     */
    private double computeUndirected(UndirectedGraph<U> undirectedGraph, U user)
    {
        return undirectedGraph.getVertexCount() - undirectedGraph.degree(user);
    }


}
