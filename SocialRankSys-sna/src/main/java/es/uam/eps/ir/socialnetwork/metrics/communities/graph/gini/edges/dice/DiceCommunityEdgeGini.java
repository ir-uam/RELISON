/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.metrics.communities.graph.gini.edges.dice;

import es.uam.eps.ir.socialnetwork.metrics.CommunityMetric;
import es.uam.eps.ir.socialnetwork.metrics.graph.EdgeGiniMode;
import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.community.graph.CommunityGraphGenerator;
import es.uam.eps.ir.socialranksys.community.graph.CompleteCommunityGraphGenerator;
import es.uam.eps.ir.socialranksys.community.graph.CompleteCommunityNoAutoloopsGraphGenerator;
import es.uam.eps.ir.socialranksys.community.graph.InterCommunityGraphGenerator;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.multigraph.DirectedMultiGraph;
import es.uam.eps.ir.socialranksys.graph.multigraph.MultiGraph;
import es.uam.eps.ir.socialranksys.utils.indexes.GiniIndex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes the Dice community edge Gini of the graph, i.e. the Gini coefficient over the
 * pairs of communities in the graph, where the frequency is the relation between the real
 * number of edges between communities, and the number of edges between communities in a 
 * configuration network where the link degree distributions are equivalent to the ones in the
 * studied network.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public abstract class DiceCommunityEdgeGini<U> implements CommunityMetric<U>
{
    /**
     * PairGini execution mode
     * @see es.uam.eps.ir.socialnetwork.metrics.graph.EdgeGiniMode
     */
    private final EdgeGiniMode mode;
    
    
    /**
     * Community graph generator.
     */
    private final CommunityGraphGenerator<U> cgg;
    
    /**
     * Constructor.
     * @param mode PairGini execution mode.
     * @param autoloops true if autoloops are allowed, false if not.
     */
    public DiceCommunityEdgeGini(EdgeGiniMode mode, boolean autoloops)
    {
        this.mode = mode;
        if(this.mode.equals(EdgeGiniMode.INTERLINKS))
            this.cgg = new InterCommunityGraphGenerator<>();
        else if(!autoloops)
            this.cgg = new CompleteCommunityGraphGenerator<>();
        else
            this.cgg = new CompleteCommunityNoAutoloopsGraphGenerator<>();
    }

    @Override
    public double compute(Graph<U> graph, Communities<U> comm)
    {
        if(graph.isDirected())
        {
            return this.computeDirected(graph, comm);
        }
        else
        {
            return this.computeUndirected(graph,comm);
        }
    }
    
    /**
     * Computes the metric for directed graphs.
     * @param graph The directed graph.
     * @param comm the community partition of the graph.
     * @return the value of the metric.
     */
    public double computeDirected(Graph<U> graph, Communities<U> comm)
    {
        DirectedMultiGraph<Integer> commGraph = (DirectedMultiGraph<Integer>) cgg.generate(graph, comm);
        Map<Integer, Double> outDegrees = new HashMap<>();
        Map<Integer, Double> inDegrees = new HashMap<>();
        
        for(int i = 0; i < comm.getNumCommunities(); ++i)
        {
            inDegrees.put(i, commGraph.inDegree(i) + 0.0);
            outDegrees.put(i, commGraph.outDegree(i) + 0.0);
        }
        
        List<Double> degrees = new ArrayList<>();
        
        double sum = 0.0;
        double sumAutoloops = 0.0;
        for(int i = 0; i < comm.getNumCommunities(); ++i)
        {
            for(int j = 0; j < comm.getNumCommunities(); ++j)
            {
                double numEdges = commGraph.getNumEdges(i, j);
                if(numEdges > 0)
                {
                    numEdges/= (outDegrees.get(i)*inDegrees.get(j));
                }
                
                if(i == j)
                {
                    sumAutoloops += numEdges;

                    if(this.mode.equals(EdgeGiniMode.COMPLETE))
                    {
                        degrees.add(numEdges);
                    }
                }
                else
                {
                    if(numEdges > 0)

                    numEdges /= (outDegrees.get(i)*inDegrees.get(j));
                }
                
                sum += numEdges;
            }
        }
        
        if(this.mode.equals(EdgeGiniMode.SEMICOMPLETE))
        {
            degrees.add(sumAutoloops);
        }
        else if(this.mode.equals(EdgeGiniMode.INTERLINKS))
        {
            sum -= sumAutoloops;
        }
        
        GiniIndex gini = new GiniIndex();
        return 1.0 - gini.compute(degrees, true, degrees.size() , sum);
    }
    
    /**
     * Computes the metric for undirected graphs.
     * @param graph The directed graph.
     * @param comm the community partition of the graph.
     * @return the value of the metric.
     */
    public double computeUndirected(Graph<U> graph, Communities<U> comm)
    {
        MultiGraph<Integer> commGraph = cgg.generate(graph, comm);
        Map<Integer, Double> sizes = new HashMap<>();
        
        for(int i = 0; i < comm.getNumCommunities(); ++i)
        {
            sizes.put(i, commGraph.degree(i) + 0.0);
        }
        
        List<Double> degrees = new ArrayList<>();
        
        double sum = 0.0;
        double sumAutoloops = 0.0;
        for(int i = 0; i < comm.getNumCommunities(); ++i)
        {
            for(int j = 0; j <= i; ++j)
            {
                double numEdges = 2.0*commGraph.getNumEdges(i, j);
                if(numEdges > 0)
                {
                    numEdges /= (sizes.get(i)*sizes.get(j));
                }
                if(i == j)
                {                   
                    sumAutoloops += numEdges;

                    if(this.mode.equals(EdgeGiniMode.COMPLETE))
                    {
                        degrees.add(numEdges);
                    }
                }
                else
                {
                    numEdges /= (sizes.get(i)*sizes.get(j));
                }
                
                sum += numEdges;
            }
        }
        
        if(this.mode.equals(EdgeGiniMode.SEMICOMPLETE))
        {
            degrees.add(sumAutoloops);
        }
        else if(this.mode.equals(EdgeGiniMode.INTERLINKS))
        {
            sum -= sumAutoloops;
        }
        
        GiniIndex gini = new GiniIndex();
        return 1.0 - gini.compute(degrees, true, degrees.size() , sum);
    }
}
