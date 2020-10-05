/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.graph;

import es.uam.eps.ir.socialranksys.graph.DirectedGraph;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.UndirectedGraph;
import es.uam.eps.ir.socialranksys.metrics.GraphMetric;

/**
 * Computes the density of a graph. It is considered that there are not
 * self-loops in the graph.
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Density<U> implements GraphMetric<U>
{
    @Override
    public double compute(Graph<U> graph)
    {
        if (graph.getVertexCount() == 0)
        {
            return 0.0;
        }
        else if (graph.isMultigraph())
        {
            return 0.0;
        }
        else if (graph.isDirected())
        {
            return this.calculateDirected((DirectedGraph<U>) graph);
        }
        else
        {
            return this.calculateUndirected((UndirectedGraph<U>) graph);
        }

    }

    /**
     * Computes the density of a directed graph.
     *
     * @param graph The directed graph
     *
     * @return The density
     */
    private double calculateDirected(DirectedGraph<U> graph)
    {
        return (graph.getEdgeCount() + 0.0) / (graph.getVertexCount() * (graph.getVertexCount() - 1.0));
    }

    /**
     * Computes the density of an undirected graph.
     *
     * @param graph The undirected graph.
     *
     * @return The density
     */
    private double calculateUndirected(UndirectedGraph<U> graph)
    {
        return 2.0 * (graph.getEdgeCount() + 0.0) / (graph.getVertexCount() * (graph.getVertexCount() - 1.0));
    }

}
