/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.metrics.communities.indiv;

import es.uam.eps.ir.socialnetwork.metrics.IndividualCommunityMetric;
import es.uam.eps.ir.socialnetwork.metrics.vertex.Degree;
import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.community.graph.InterCommunityGraphGenerator;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.multigraph.MultiGraph;

import java.util.Map;

/**
 * Computes the community degree.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public class CommunityDegree<U> implements IndividualCommunityMetric<U>
{

    /**
     * Indicates if the degree to obtain is inDegree, outDegree or the
     * full degree of the community graph.
     */
    private final EdgeOrientation orientation;
    
    /**
     * Constructor.
     * @param orientation Indicates if the degree to obtain is inDegree, outDegree or the
     * full degree of the community graph.
     */
    public CommunityDegree(EdgeOrientation orientation)
    {
        this.orientation = orientation;
    }
    
    @Override
    public double compute(Graph<U> graph, Communities<U> comm, int indiv)
    {
        InterCommunityGraphGenerator<U> cgg = new InterCommunityGraphGenerator<>();
        MultiGraph<Integer> commGraph = cgg.generate(graph, comm);
        
        Degree<Integer> degree = new Degree<>(orientation);
        return degree.compute(commGraph, indiv);
    }

    @Override
    public Map<Integer, Double> compute(Graph<U> graph, Communities<U> comm) 
    {
        InterCommunityGraphGenerator<U> cgg = new InterCommunityGraphGenerator<>();
        MultiGraph<Integer> commGraph = cgg.generate(graph, comm);
        
        Degree<Integer> degree = new Degree<>(orientation);
        
        return degree.compute(commGraph);
    }

    @Override
    public double averageValue(Graph<U> graph, Communities<U> comm) 
    {
        InterCommunityGraphGenerator<U> cgg = new InterCommunityGraphGenerator<>();
        MultiGraph<Integer> commGraph = cgg.generate(graph, comm);
        
        Degree<Integer> degree = new Degree<>(orientation);
        
        return degree.averageValue(commGraph);
    }
    
}
