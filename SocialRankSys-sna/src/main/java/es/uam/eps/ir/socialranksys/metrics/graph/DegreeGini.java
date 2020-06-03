/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
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
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.metrics.GraphMetric;
import es.uam.eps.ir.socialranksys.utils.indexes.GiniIndex;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Computes the Gini metric between the different nodes in the graph (auto-nodes are
 * not taken into account). This metric tries to see how equally the degree is
 * distributed between the different nodes.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class DegreeGini<U> implements GraphMetric<U>
{

    /**
     * Orientation of the edges
     */
    private final EdgeOrientation orientation;
    
    /**
     * Constructor.
     * @param orientation Orientation of the edges.
     */
    public DegreeGini(EdgeOrientation orientation)
    {
        this.orientation = orientation;
    }
    
    /**
     * Computes the degree Gini. It is computed only if there is, at least, two nodes.
     * If not, NaN is returned. If there is no edges, then, the value is equal to 1.0 (every
     * node has the same degree).
     * @param graph the graph
     * @return the computed value.
     */
    @Override
    public double compute(Graph<U> graph)
    {
        if(graph.getVertexCount() < 1.0)
            return Double.NaN;
        if(graph.getEdgeCount() == 0) // every node is equal to zero
            return 1.0;
        if(graph.isDirected())
            return computeDirected((DirectedGraph<U>) graph);
        return computeUndirected((UndirectedGraph<U>) graph);
    }
    
    /**
     * Computes the Degree Gini for the directed graph case
     * @param graph The directed graph.
     * @return The value of the metric.
     */
    private double computeDirected(DirectedGraph<U> graph)
    {
        List<Double> degrees;
        
        
        
        if(this.orientation.equals(EdgeOrientation.IN))
        {
            degrees = graph.getAllNodes().map((node) -> graph.inDegree(node)+0.0).sorted().collect(Collectors.toCollection(ArrayList::new));
        }
        else if(this.orientation.equals(EdgeOrientation.OUT))
        {
            degrees = graph.getAllNodes().map((node) -> graph.outDegree(node)+0.0).sorted().collect(Collectors.toCollection(ArrayList::new));
        }
        else
        {
            degrees = graph.getAllNodes().map((node) -> graph.inDegree(node)+graph.outDegree(node) + 0.0).sorted().collect(Collectors.toCollection(ArrayList::new));
        }
        
        long vertexCount = graph.getVertexCount();
        double sum = degrees.stream().mapToDouble(u -> u).sum();
        GiniIndex gi = new GiniIndex();
        
        double value = gi.compute(degrees, false, vertexCount, sum);
        return 1.0 - value;
    }
    
    
    
    /**
     * Computes the Degree Gini for the undirected graph case
     * @param graph The undirected graph
     * @return The value of the metric.
     */
    private double computeUndirected(UndirectedGraph<U> graph)
    {
        List<Double> degrees = graph.getAllNodes().map((comm) -> graph.degree(comm)+0.0)
            .sorted().collect(Collectors.toCollection(ArrayList::new));
        long vertexCount = graph.getVertexCount();

        GiniIndex gi = new GiniIndex();
        double sum = degrees.stream().mapToDouble(u -> u).sum();
        double value = gi.compute(degrees, false, vertexCount, sum);
        return 1.0 - value;
    }
}
