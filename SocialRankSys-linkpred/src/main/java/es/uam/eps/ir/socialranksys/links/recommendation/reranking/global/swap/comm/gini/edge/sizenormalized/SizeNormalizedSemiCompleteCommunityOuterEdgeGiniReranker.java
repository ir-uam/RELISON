/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
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

import static java.lang.Integer.max;
import static java.lang.Integer.min;

/**
 * Reranks a recommendation by improving the Gini Index of the distribution of edges
 * between communities. Both edges between different communities and links inside of 
 * communities are considered, but links between communities are stored in a different
 * group when computing Gini coefficient.
 * 
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public class SizeNormalizedSemiCompleteCommunityOuterEdgeGiniReranker<U> extends CommunityReranker<U>
{

    /**
     * Number of edges between communities
     */
    private final List<Double> matrix;
    /**
     * Number of edges between communities.
     */
    private double sum;
    /**
     * Map that stores the size of different communities.
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
     * @param autoloops true if autoloops are allowed, false otherwise.
     */
    public SizeNormalizedSemiCompleteCommunityOuterEdgeGiniReranker(double lambda, int cutoff, boolean norm, boolean rank, Graph<U> graph, Communities<U> communities, boolean autoloops)
    {
        super(lambda, cutoff, norm, rank,  graph, communities, autoloops);
        
        matrix = new ArrayList<>();
        sum = 0.0;
        commSizes = new HashMap<>();
        communities.getCommunities().forEach(c -> commSizes.put(c,communities.getCommunitySize(c)+0.0));
    }

    @Override
    protected void update(Recommendation<U, U> reranked) {
    }

    @Override
    protected double novAddDelete(U u, Tuple2od<U> itemValue, Tuple2od<U> compared)
    {
        U recomm = itemValue.v1;
        U del = compared.v1;

        int numComm = communities.getNumCommunities();
        int userComm = communities.getCommunity(u);
        int recommComm = communities.getCommunity(recomm);
        int delComm = communities.getCommunity(del);

        GiniIndex gini = new GiniIndex();
        
        int newIdx = findIndex(userComm, recommComm);
        int delIdx = findIndex(userComm, delComm);

        if(userComm == recommComm)
            return -1.0;
        
        DoubleStream stream = IntStream.range(0, matrix.size()).mapToDouble(index -> 
        {
            double value = matrix.get(index);

            if(index == newIdx)
            {
                value += valueToAdd(userComm, recommComm);
            }

            if(index == delIdx)
            {
                value -= valueToAdd(userComm, delComm);
            }

            return value;
        });
        
        double sumAdd = valueToAdd(userComm, recommComm) - valueToAdd(userComm, delComm);
        int numPairs = this.graph.isDirected() ? (numComm*(numComm - 1) + 1) : (numComm*(numComm - 1)/2 + 1);

        return 1.0 - gini.compute(stream.boxed(), true, numPairs, this.sum + sumAdd);
    }
    
    @Override
    protected double novAdd(U u, Tuple2od<U> itemValue, Tuple2od<U> compared)
    {
        U recomm = itemValue.v1;
        U del = compared.v1;

        int numComm = communities.getNumCommunities();
        int userComm = communities.getCommunity(u);
        int recommComm = communities.getCommunity(recomm);
        int delComm = communities.getCommunity(del);

        GiniIndex gini = new GiniIndex();
        
        int newIdx = findIndex(userComm, recommComm);
        
        if(userComm == recommComm)
            return -1.0;
        
        DoubleStream stream = IntStream.range(0, matrix.size()).mapToDouble(index -> 
        {
            double value = matrix.get(index);

            if(index == newIdx)
            {
                value += valueToAdd(userComm, recommComm);
            }

            return value;
        });
        
        int numPairs = this.graph.isDirected() ? (numComm*(numComm - 1) + 1) : (numComm*(numComm - 1)/2 + 1);

        return 1.0 - gini.compute(stream.boxed(), true, numPairs, this.sum + valueToAdd(userComm, recommComm));
    }
    
    @Override
    protected double novDelete(U u, Tuple2od<U> itemValue, Tuple2od<U> compared)
    {
        U recomm = itemValue.v1;
        U del = compared.v1;

        int numComm = communities.getNumCommunities();
        int userComm = communities.getCommunity(u);
        int recommComm = communities.getCommunity(recomm);
        int delComm = communities.getCommunity(del);

        GiniIndex gini = new GiniIndex();
        
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
        
        int numPairs = this.graph.isDirected() ? (numComm*(numComm - 1) + 1) : (numComm*(numComm - 1)/2 + 1);

        return 1.0 - gini.compute(stream.boxed(), true, numPairs, this.sum - valueToAdd(userComm, delComm));
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
        int numPairs = graph.isDirected() ? numComm*(numComm-1)+1 : (numComm*(numComm-1)/2+1);
        
        int newIdx = findIndex(userComm, recommComm);
        int delIdx = findIndex(userComm, delComm);
                
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
        matrix.set(delIdx, matrix.get(delIdx) + addDel);
        
        this.globalvalue = 1.0 - gini.compute(matrix, true, numPairs, this.sum);
    }

    @Override
    protected void computeGlobalValue() 
    {
        GiniIndex gini = new GiniIndex();

        super.computeGlobalValue();
        long vertexcount = communityGraph.getVertexCount();        
        
        if(communityGraph.isDirected())
        {
            sum = 0.0;
            double autoloopsSum = 0.0;
            for(int i = 0; i < vertexcount; ++i)
            {
                double sizeI = commSizes.get(i);
                for(int j = 0; j < vertexcount; ++j)
                {
                    double sizeJ = commSizes.get(j);
                    double value;
                    if(i == j)
                    {
                        if(autoloops)
                        {
                            value = this.communityGraph.getNumEdges(i,i)/(sizeI*sizeI + 0.0);
                            autoloopsSum += value;
                        }
                        else
                        {
                            value = this.communityGraph.getNumEdges(i,i)/Math.max(1.0, sizeI*(sizeI - 1.0));
                            autoloopsSum += value;
                        }
                    }
                    else
                    {
                        value = this.communityGraph.getNumEdges(i, j)/(sizeI*sizeJ + 0.0);
                        matrix.add(value);
                        sum += value;
                    }
                }
            }
            
            matrix.add(autoloopsSum);
            sum+=autoloopsSum;
            this.globalvalue = 1.0 - gini.compute(matrix, true, communities.getNumCommunities()*(communities.getNumCommunities()-1)+1, this.sum);
        }
        else
        {
            sum = 0.0;
            for(int i = 0; i < vertexcount; ++i)
            {
                double sizeI = commSizes.get(i);
                for(int j = i+1; j < vertexcount; ++j)
                {
                    double sizeJ = commSizes.get(j);
                    double value = (communityGraph.getNumEdges(i, j)+0.0)/(sizeI*sizeJ + 0.0);
                    matrix.add(value);
                    sum+=value;
                }
            }
            double autoloopsSum = 0.0;
            for(int i = 0; i < vertexcount; ++i)
            {
                double sizeI = commSizes.get(i);
                if(autoloops)
                {
                    autoloopsSum += 2.0*(this.communityGraph.getNumEdges(i, i)+0.0)/(sizeI*(sizeI+1.0));
                }
                else
                {
                    autoloopsSum += 2.0*(this.communityGraph.getNumEdges(i, i)+0.0)/Math.max(1.0, sizeI*(sizeI - 1.0));
                }
                autoloopsSum += communityGraph.getNumEdges(i, i);
            }
            sum += autoloopsSum;
            matrix.add(autoloopsSum);
            this.globalvalue = 1.0 - gini.compute(matrix, true, communities.getNumCommunities()*(communities.getNumCommunities()-1)/2 + 1, this.sum);
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
        
        if(userComm == recommComm)
        {
            return matrix.size() - 1;
        }
        else if(this.graph.isDirected())
        {
            return userComm*numComm + recommComm - userComm - ((userComm < recommComm)? 1 : 0);
        }
        else
        {            
            int auxNewIdx = 0;

            for(int i = 0; i < min(userComm, recommComm); ++i)
            {
                auxNewIdx += numComm - i - 1;
            }
            auxNewIdx += max(userComm, recommComm) - min(userComm, recommComm) - 1;
            return auxNewIdx;
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
            else if(graph.isDirected()) // && !autoloopsSum
            {
                return 1.0/(this.commSizes.get(firstComm)*(this.commSizes.get(secondComm) - 1.0));
            }
            else if(autoloops) // !graph.isDirected()
            {
                return 2.0/(this.commSizes.get(firstComm)*(this.commSizes.get(secondComm) + 1.0));
            }
            else // Undirected graph, without allowing autoloopsSum
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
