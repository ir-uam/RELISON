/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.swap.comm.gini.edge;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.swap.comm.CommunityReranker;
import es.uam.eps.ir.socialranksys.utils.indexes.GiniIndex;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * Reranks a recommendation by improving the Gini Index of the distribution of edges
 * between communities. Both edges between different communities and links inside of 
 * communities are considered.
 * 
 * This algorithm also looks promoting the existence of inter-link communities, so, when we add
 * new links, the reranking score will be -1.0 if the new link goes to the same community than 
 * the user whose recommendation we are reranking, or the variation in Gini in other case.
 * 
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public class CompleteCommunityOuterEdgeGiniReranker<U> extends CommunityReranker<U>
{
    /**
     * Number of links between communities.
     */
    private final List<Double> matrix;
    /**
     * Total number of links between communities.
     */
    private double sum = 0.0;

    /**
     * Constructor.
     * @param lambda Establishes the trait-off between the value of the Gini Index and the original score
     * @param cutoff The number of recommended items for each user.
     * @param norm true if the score and the Gini Index value have to be normalized.
     * @param rank true if the normalization is by ranking position, false if it is by score
     * @param graph The user graph.
     * @param communities A relation between communities and users in the graph.
     * @param autoloops true if autoloops are allowed, false otherwise.
     */
    public CompleteCommunityOuterEdgeGiniReranker(double lambda, int cutoff, boolean norm, boolean rank, Graph<U> graph, Communities<U> communities, boolean autoloops) {
        super(lambda, cutoff, norm, rank, graph, communities, autoloops);
        
        this.matrix = new ArrayList<>();
        
    }
  
    @Override
    protected double novAddDelete(U u, Tuple2od<U> newItem, Tuple2od<U> oldItem)
    {
        U recomm = newItem.v1();
        U del = oldItem.v1();
        
        GiniIndex gini = new GiniIndex();
        int userComm = communities.getCommunity(u);
        int recommComm = communities.getCommunity(recomm);
        int delComm = communities.getCommunity(del);
        
        int numComm = communities.getNumCommunities();
        
        if(userComm == recommComm)
            return -1.0;
        
        int newIdx = findIndex(userComm, recommComm);
        int oldIdx = findIndex(userComm, delComm);
        
        DoubleStream stream = IntStream.range(0, matrix.size()).mapToDouble(index -> 
        {
            double value = matrix.get(index);
            if(index == newIdx)
            {
                value += 1;
            }
            
            if(index == oldIdx)
            {
                value -= 1;
            }
            
            return value;
        });
        
        int numPairs = graph.isDirected() ? numComm*numComm : numComm*numComm/2;

        return 1.0 - gini.compute(stream.boxed(), true, numPairs, this.communityGraph.getEdgeCount() + 0.0);
    }

    @Override
    protected double novAdd(U u, Tuple2od<U> newItem, Tuple2od<U> oldItem)
    {
        U recomm = newItem.v1();
        U del = oldItem.v1();
        
        
        GiniIndex gini = new GiniIndex();
        int userComm = communities.getCommunity(u);
        int recommComm = communities.getCommunity(recomm);
        
        if(userComm == recommComm)
            return -1.0;
        
        int numComm = communities.getNumCommunities();
        int numPairs = numComm*numComm/2;
        
        int minNew = Math.min(userComm, recommComm);
        int maxNew = Math.max(userComm, recommComm);

        int newIdx = findIndex(userComm, recommComm);

        DoubleStream stream = IntStream.range(0, matrix.size()).mapToDouble(index -> 
        {
            double value = matrix.get(index);
            if(index == newIdx)
            {
                value += 1.0;
            }
            
            return value;
        });

        return 1.0 - gini.compute(stream.boxed(), true, numPairs, this.sum + 1.0);
    }

    @Override
    protected double novDelete(U u, Tuple2od<U> newItem, Tuple2od<U> oldItem)
    {
        U recomm = newItem.v1();
        U del = oldItem.v1();
        
        GiniIndex gini = new GiniIndex();
        int userComm = communities.getCommunity(u);
        int delComm = communities.getCommunity(del);
        
        int numComm = communities.getNumCommunities();
        int numPairs = numComm*numComm/2;
        
        int minDel = Math.min(userComm, delComm);
        int maxDel = Math.max(userComm, delComm);

        int delIdx = findIndex(userComm, delComm);
        
        DoubleStream stream = IntStream.range(0, matrix.size()).mapToDouble(index -> 
        {
            double value = matrix.get(index);
            if(index == delIdx)
            {
                value -= 1.0;
            }
            
            return value;
        });

        return 1.0 - gini.compute(stream.boxed(), true, numPairs, this.sum - 1.0);
    }

    @Override
    protected void update(Recommendation<U, U> reranked)
    {
        
    }

    @Override
    protected void innerUpdate(U user, Tuple2od<U> updated, Tuple2od<U> old)
    {
        U recomm = updated.v1();
        U del = old.v1();
        
        GiniIndex gini = new GiniIndex();
        int userComm = communities.getCommunity(user);
        int recommComm = communities.getCommunity(recomm);
        int delComm = communities.getCommunity(del);
        
        int numComm = communities.getNumCommunities();
        int numPairs = graph.isDirected() ? numComm*numComm : (numComm*numComm/2);
        
        int newIdx = findIndex(userComm, recommComm);
        int oldIdx = findIndex(userComm, delComm);
        
        double addNew;
        double addDel;
        double sumVar;
        
        if(this.graph.isDirected())
        {
            addNew = 1.0;
            addDel = -1.0;
        }
        else if(recs.get(del).contains(user) && recs.get(recomm).contains(user))
        {
            addNew = 0.0;
            addDel = 0.0;
        }
        else if(recs.get(recomm).contains(user))
        {
            addNew = 1.0;
            addDel = 0.0;
        }
        else if(recs.get(del).contains(user))
        {
            addNew = 0.0;
            addDel = -1.0;
        }
        else
        {
            addNew = 1.0;
            addDel = -1.0;
        }
        
        sumVar = addNew + addDel;
        this.sum += sumVar;
        
        matrix.set(newIdx, matrix.get(newIdx) + addNew);
        matrix.set(oldIdx, matrix.get(oldIdx) + addDel);
        
        this.globalvalue = 1.0 - gini.compute(matrix, true, numPairs, this.sum);
    }

    @Override
    protected void computeGlobalValue()
    {
        super.computeGlobalValue();
        GiniIndex gini = new GiniIndex();
        
        long vertexcount = this.communityGraph.getVertexCount();
        this.sum = this.communityGraph.getEdgeCount() + 0.0;
                
        if(this.communityGraph.isDirected())
        {
            for(int i = 0; i < vertexcount; ++i)
            {
                for(int j = 0; j < vertexcount; ++j)
                {
                    matrix.add(this.communityGraph.getNumEdges(i, j)+0.0);
                }
            }
            
            this.globalvalue = 1.0 - gini.compute(matrix, true, communities.getNumCommunities()*communities.getNumCommunities(), sum);
        }
        else
        {
            for(int i = 0; i < vertexcount; ++i)
            {
                for(int j=i; j < vertexcount; ++j)
                {
                    matrix.add(this.communityGraph.getNumEdges(i,j) + 0.0);
                }
            }
            this.globalvalue = 1.0 - gini.compute(matrix, true, vertexcount*(vertexcount+1)/2, sum);
        }
    }
    
    /**
     * Finds the index for a pair of communities in the matrix
     * @param userComm the origin endpoint
     * @param recommComm the destiny endpoint
     * @return the index in the matrix, -1 if it does not exist.
     */
    private int findIndex(int userComm, int recommComm)
    {
        
        int numComm = this.communities.getNumCommunities();
        if(userComm < 0 || userComm >= numComm || recommComm < 0 || recommComm >= numComm)
        {
            return -1;
        }
        
        if(this.communityGraph.isDirected())
        {
            return userComm*numComm + recommComm;
        }
        else
        {
            int minNew = Math.min(userComm, recommComm);
            int maxNew = Math.max(userComm, recommComm);
            
            return numComm*minNew - minNew*(minNew-1)/2 + (maxNew - minNew);           
        }
    }
}
