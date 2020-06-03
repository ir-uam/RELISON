/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.communities.graph;

import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.community.graph.CommunityGraphGenerator;
import es.uam.eps.ir.socialranksys.community.graph.InterCommunityGraphGenerator;
import es.uam.eps.ir.socialranksys.graph.DirectedGraph;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.UndirectedGraph;
import es.uam.eps.ir.socialranksys.graph.multigraph.MultiGraph;
import es.uam.eps.ir.socialranksys.metrics.CommunityMetric;

/**
 * Computes the modularity of a graph, given the communities.
 * 
 * Newman, M.E.J., Girvan, M. Finding and evaluating community structure in networks. Physical Review E 69(2), pp. 1-16 (2004) 
 * 
 * @author Javier Sanz-Cruzado Puig
 * @param <U> The type of the users.
 */
public class Modularity<U> implements CommunityMetric<U>
{

    @Override
    public double compute(Graph<U> graph, Communities<U> comm) {
        CommunityGraphGenerator<U> cgg = new InterCommunityGraphGenerator<>();
        MultiGraph<Integer> commGraph = cgg.generate(graph, comm);
        
        
        if(graph.isDirected())
            return this.computeDirected((DirectedGraph<U>) graph, comm, commGraph);
        else
            return this.computeUndirected((UndirectedGraph<U>) graph, comm, commGraph);
    }
    
    /**
     * Computes the value of the modularity for a directed graph
     * @param graph The directed graph
     * @param comm The communities
     * @param commGraph The community graph
     * @return The value of the modularity
     */
    private double computeDirected(DirectedGraph<U> graph, Communities<U> comm, MultiGraph<Integer> commGraph)
    {
        double modularity;
        // Compute \sum_i,j |\Gamma_out(i)||\Gamma_in(j)|\delta(c_i, c_j)
        double k = graph.getAllNodes().map(orig -> 
        {
            double userSum = 0.0;
            int commOrig = comm.getCommunity(orig);
            userSum = graph.getAllNodes().map(dest -> 
            {
                double value = 0.0;
                int commDest = comm.getCommunity(dest);
                if(commOrig == commDest)
                {
                    value = graph.outDegree(orig)*graph.inDegree(dest) + 0.0;
                }
                return value;
            }).reduce(userSum, Double::sum);
            return userSum;
        }).reduce(0.0, Double::sum);
        
        long numEdges = graph.getEdgeCount();
        modularity = numEdges - commGraph.getEdgeCount();
        modularity -= k/(numEdges + 0.0);
        modularity /= (numEdges - k/(numEdges +0.0) + 0.0);
        return modularity;
    }

    /**
     * Computes the value of the modularity for an undirected graph.
     * @param graph The undirected graph.
     * @param comm The communities.
     * @param commGraph The community graph.
     * @return The value of the modularity of the graph.
     */
    private double computeUndirected(UndirectedGraph<U> graph, Communities<U> comm, MultiGraph<Integer> commGraph)
    {
        double modularity;
        // Compute \sum_i,j |\Gamma_out(i)||\Gamma_in(j)|\delta(c_i, c_j)
        double k = graph.getAllNodes().map(orig -> 
        {
            double userSum = 0.0;
            int commOrig = comm.getCommunity(orig);
            userSum = graph.getAllNodes().map(dest -> 
            {
                double value = 0.0;
                int commDest = comm.getCommunity(dest);
                if(commOrig == commDest)
                {
                    value = graph.degree(orig)*graph.degree(dest) + 0.0;
                }
                return value;
            }).reduce(userSum, Double::sum);
            return userSum;
        }).reduce(0.0, Double::sum);
        
        long numEdges = 2*graph.getEdgeCount();
        modularity = numEdges - commGraph.getEdgeCount();
        modularity -= k/(numEdges + 0.0);
        modularity /= (numEdges - k/(numEdges +0.0) + 0.0);
        return modularity;
    }
}
