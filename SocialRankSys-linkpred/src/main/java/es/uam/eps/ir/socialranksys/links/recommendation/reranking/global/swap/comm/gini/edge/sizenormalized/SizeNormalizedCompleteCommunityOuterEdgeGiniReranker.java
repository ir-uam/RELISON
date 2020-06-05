/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.swap.comm.gini.edge.sizenormalized;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.swap.comm.CommunityReranker;
import es.uam.eps.ir.socialranksys.utils.indexes.GiniIndex;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * Reranks a recommendation by improving the Size-Normalized Gini Index of the distribution of edges
 * between communities. Both edges between different communities and links inside of 
 * communities are considered.
 * 
 * This algorithm also looks promoting the existence of inter-link communities, so, when we add
 * new links, the reranking score will be -1.0 if the new link goes to the same community than 
 * the user whose recommendation we are reranking, or the variation in Gini in other case.
 * 
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 * 
 */
public class SizeNormalizedCompleteCommunityOuterEdgeGiniReranker<U> extends CommunityReranker<U>
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
     * Map that stores the sizes of the different communities.
     */
    private final Map<Integer, Double> commSizes;

    /**
     * Constructor.
     * @param lambda Establishes the trait-off between the value of the Gini Index and the original score
     * @param cutoff The number of recommended items for each user.
     * @param norm true if the score and the Gini Index value have to be normalized.
     * @param rank true if the normalization is by ranking position, false if it is by score
     * @param graph The user graph.
     * @param communities A relation between communities and users in the graph.
     * @param autoloops true if autoloops are allowed or false otherwise.
     */
    public SizeNormalizedCompleteCommunityOuterEdgeGiniReranker(double lambda, int cutoff, boolean norm, boolean rank, Graph<U> graph, Communities<U> communities, boolean autoloops) {
        super(lambda, cutoff, norm, rank, graph, communities, autoloops);
        
        this.matrix = new ArrayList<>();
        this.commSizes = new HashMap<>();
        
        communities.getCommunities().forEach(c -> commSizes.put(c, communities.getCommunitySize(c)+0.0));
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
        
        if(userComm == recommComm)
            return -1.0;
        int numComm = communities.getNumCommunities();
        
        int newIdx = findIndex(userComm, recommComm);
        int oldIdx = findIndex(userComm, delComm);       
        
        DoubleStream stream = IntStream.range(0, matrix.size()).mapToDouble(index -> 
        {
            double value = matrix.get(index);
            if(index == newIdx)
            {
                value += valueToAdd(userComm, recommComm);
            }
            
            if(index == oldIdx)
            {
                value -= valueToAdd(userComm, delComm);
            }
            
            return value;
        });
        
        double sumMod = valueToAdd(userComm, recommComm) - valueToAdd(userComm, delComm);
        int numPairs = graph.isDirected() ? numComm*numComm : numComm*numComm/2;

        return 1.0 - gini.compute(stream.boxed(), true, numPairs, this.sum + sumMod);
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
                value += valueToAdd(userComm, recommComm);
            }
            
            return value;
        });

        return 1.0 - gini.compute(stream.boxed(), true, numPairs, this.sum + 1.0/(this.commSizes.get(userComm)* this.commSizes.get(recommComm)+0.0));
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
                value -= valueToAdd(userComm, delComm);
            }
            
            return value;
        });

        return 1.0 - gini.compute(stream.boxed(), true, numPairs, this.sum - 1.0/(this.commSizes.get(userComm)* this.commSizes.get(delComm) + 0.0));
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
            addNew = valueToAdd(userComm, recommComm);
            addDel = -valueToAdd(userComm, delComm);
        }
        else if(recs.get(del).contains(user) && recs.get(recomm).contains(user))
        {
            addNew = 0.0;
            addDel = 0.0;
        }
        else if(recs.get(recomm).contains(user))
        {
            addNew = valueToAdd(userComm, recommComm);
            addDel = 0.0;
        }
        else if(recs.get(del).contains(user))
        {
            addNew = 0.0;
            addDel = -valueToAdd(userComm, delComm);
        }
        else
        {
            addNew = valueToAdd(userComm, recommComm);
            addDel = -valueToAdd(userComm, delComm);
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
        this.sum = this.communityGraph.getAllNodes().mapToDouble(u -> 
        {
            double sizeU = this.commSizes.get(u);
            return this.communityGraph.getAdjacentNodes(u).mapToDouble(v -> 
            {
                double sizeV = this.commSizes.get(v);
                return this.communityGraph.getNumEdges(u, v)/(sizeU*sizeV + 0.0);
            }).sum();
        }).sum();
        
        this.sum = 0.0;
        if(this.communityGraph.isDirected())
        {
            for(int i = 0; i < vertexcount; ++i)
            {
                double sizeI = this.commSizes.get(i);
                for(int j = 0; j < vertexcount; ++j)
                {
                    double sizeJ = this.commSizes.get(j);
                    
                    double value;
                    if(i == j)
                    {
                        if(autoloops)
                            value = this.communityGraph.getNumEdges(i, i)/(sizeI*sizeI + 0.0);
                        else
                            value = this.communityGraph.getNumEdges(i, i)/Math.max(1.0, sizeI*(sizeI - 1.0));
                    }
                    else
                    {
                        value = (this.communityGraph.getNumEdges(i, j)+0.0)/(sizeI*sizeJ + 0.0);
                    }
                    matrix.add(value);
                    this.sum += value;
                }
            }
            this.globalvalue = 1.0 - gini.compute(matrix, true, communities.getNumCommunities()*communities.getNumCommunities(), sum);
        }
        else
        {
            for(int i = 0; i < vertexcount; ++i)
            {
                double sizeI = this.commSizes.get(i);
                for(int j=i; j < vertexcount; ++j)
                {
                    double value;
                    if(i == j)
                    {
                        if(autoloops)
                        {
                            value = 2.0*(this.communityGraph.getNumEdges(i, j)+0.0)/(sizeI*(sizeI + 1.0));
                        }
                        else
                        {
                            value = 2.0*(this.communityGraph.getNumEdges(i, j)+0.0)/Math.max(1.0, sizeI*(sizeI - 1.0));
                        }
                    }
                    else
                    {
                        double sizeJ = this.commSizes.get(j);
                        value = (this.communityGraph.getNumEdges(i, j)+0.0)/(sizeI*sizeJ + 0.0);
                    }
                    matrix.add(value);
                    this.sum += value;                
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
    
    /**
     * Auxiliary function that computes the variation in the value of adding a new edge between
     * two communities (or deleting it).
     * @param firstComm the first community
     * @param secondComm the second community
     * @return the variation.
     */
    private double valueToAdd(int firstComm, int secondComm)
    {
        if(firstComm == secondComm)
        {
            if(graph.isDirected() && autoloops)
            {
                return 1.0/(this.commSizes.get(firstComm)*this.commSizes.get(secondComm) + 0.0);
            }
            else if(graph.isDirected()) // && !autoloops
            {
                return 1.0/(this.commSizes.get(firstComm)*(this.commSizes.get(secondComm) - 1.0));
            }
            else if(autoloops) // !graph.isDirected()
            {
                return 2.0/(this.commSizes.get(firstComm)*(this.commSizes.get(secondComm) + 1.0));
            }
            else // Undirected graph, without allowing autoloops
            {
                return 2.0/(this.commSizes.get(firstComm)*(this.commSizes.get(secondComm) - 1.0));
            }
        }
        else
        {
            return 1.0/(this.commSizes.get(firstComm)*this.commSizes.get(secondComm) + 0.0);
        }
    }
}
