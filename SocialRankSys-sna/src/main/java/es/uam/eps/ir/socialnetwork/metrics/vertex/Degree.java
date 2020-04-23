/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.metrics.vertex;

import es.uam.eps.ir.socialnetwork.metrics.VertexMetric;
import es.uam.eps.ir.socialranksys.graph.DirectedGraph;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.UndirectedGraph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;

/**
 * Computes the degree of a given user in a graph
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class Degree<U> implements VertexMetric<U>
{
    /**
     * The orientation in which the score is computed.
     */
    private final EdgeOrientation orientation;
    
    /**
     * Constructor.
     * @param orientation The orientation in which to take the value 
     */
    public Degree(EdgeOrientation orientation)
    {
        this.orientation = orientation;
    }
    
    @Override
    public double compute(Graph<U> graph, U user) {
        if(graph.isDirected())
            return this.computeDirected((DirectedGraph<U>) graph, user);
        else // the graph is not directed
            return this.computeUndirected((UndirectedGraph<U>) graph, user);
    }
    
    @Override
    public double averageValue(Graph<U> graph) {
        if(graph.getVertexCount() > 0)
            if(graph.isDirected())
                return (graph.getEdgeCount() + 0.0)/(graph.getVertexCount() + 0.0);
            else
                return 2.0*(graph.getEdgeCount() + 0.0)/(graph.getVertexCount() + 0.0);
        return 0.0;
    }

    /**
     * Computes the degree of the user in a directed graph.
     * @param directedGraph The directed graph to take.
     * @param user The user.
     * @return the corresponding degree value.
     */
    private double computeDirected(DirectedGraph<U> directedGraph, U user) 
    {
        if(orientation.equals(EdgeOrientation.IN))
            return directedGraph.inDegree(user);
        else if(orientation.equals(EdgeOrientation.OUT))
            return directedGraph.outDegree(user);
        else
            return directedGraph.degree(user);
    }

    /**
     * Computes the degree of the user in an undirected graph.
     * @param undirectedGraph The undirected graph to take.
     * @param user The user.
     * @return the degree of the user in that graph.
     */
    private double computeUndirected(UndirectedGraph<U> undirectedGraph, U user) {
        return undirectedGraph.degree(user);
    }
    
    


    
}
