/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.metrics.graph;

import es.uam.eps.ir.socialnetwork.metrics.GraphMetric;
import es.uam.eps.ir.socialranksys.graph.DirectedGraph;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.UndirectedGraph;

/**
 * Computes the density of a graph. It is considered that there are not 
 * autoedges in the graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class Density<U> implements GraphMetric<U> 
{
    @Override
    public double compute(Graph<U> graph)
    {
        if(graph.getVertexCount() == 0)
            return 0.0;
        else if(graph.isMultigraph())
            return 0.0;
        else if(graph.isDirected())
            return this.calculateDirected((DirectedGraph<U>) graph);
        else
            return this.calculateUndirected((UndirectedGraph<U>) graph);
        
    }

    /**
     * Computes the density of a directed graph.
     * @param graph The directed graph
     * @return The density
     */
    private double calculateDirected(DirectedGraph<U> graph) 
    {
        return (graph.getEdgeCount() + 0.0)/(graph.getVertexCount()*(graph.getVertexCount()-1.0));
    }

    /**
     * Computes the density of an undirected graph.
     * @param graph The undirected graph.
     * @return The density
     */
    private double calculateUndirected(UndirectedGraph<U> graph) 
    {
        return 2.0*(graph.getEdgeCount() + 0.0)/(graph.getVertexCount()*(graph.getVertexCount()-1.0));
    }
    
}
