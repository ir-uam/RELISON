/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
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

import static java.lang.Integer.max;
import static java.lang.Integer.min;

/**
 * Reranks a recommendation by improving the Gini Index of the distribution of edges
 * between communities. Only edges between different communities are considered.
 * 
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public class InterCommunityOuterEdgeGiniReranker<U> extends CommunityReranker<U>
{
    /**
     * Number of edges between communities
     */
    private final List<Double> matrix;
    /**
     * Total number of edges between communities
     */
    private double sum;
    
    /**
     * Constructor.
     * @param lambda Establishes the trait-off between the value of the Gini Index and the original score
     * @param cutoff The number of recommended items for each user.
     * @param norm true if the score and the Gini Index value have to be normalized.
     * @param rank true if the normalization is by ranking position, false if it is by score
     * @param graph The user graph.
     * @param communities A relation between communities and users in the graph.
     */
    public InterCommunityOuterEdgeGiniReranker(double lambda, int cutoff, boolean norm, boolean rank, Graph<U> graph, Communities<U> communities)
    {
        super(lambda, cutoff, norm, rank, graph, communities, false);
        
        matrix = new ArrayList<>();
        sum = 0.0;
    }

    @Override
    protected void update(Recommendation<U, U> reranked) {
    }

    @Override
    protected double novAddDelete(U u, Tuple2od<U> itemValue, Tuple2od<U> compared)
    {
        U recomm = itemValue.v1;
        U del = compared.v1;

        int userComm = communities.getCommunity(u);
        int recommComm = communities.getCommunity(recomm);
        int delComm = communities.getCommunity(del);

        GiniIndex gini = new GiniIndex();
        
        if(userComm == recommComm)
        {
            return -1.0;
        }
        // The same edge between communities is added and delete.
        if(recommComm == delComm)
        {
            return this.globalvalue;
        }
        
        // Find the index for the new edge to add and the edge to delete in the matrix.
        int newIdx = findIndex(userComm, recommComm);
        int delIdx = findIndex(userComm, delComm);
        
        double incrNew = 1.0;
        double incrOld = userComm == delComm ? 0.0 : -1.0;
        double sumVar = incrNew + incrOld;
        int numComms = this.communities.getNumCommunities();
        
        // Number of community pairs.
        int numPairs = communityGraph.isDirected() ? numComms*(numComms-1) : numComms*(numComms-1)/2;
        
        // Find the value of the metric.
        DoubleStream stream = IntStream.range(0, matrix.size()).mapToDouble(index -> 
        {
            double value = matrix.get(index);
            if(index == newIdx)
            {
                value += incrNew;
            }
            
            if(index == delIdx)
            {
                value += incrOld;
            }
                       
            return value;
        });

        return 1.0 - gini.compute(stream.boxed(), true, numPairs, sum + sumVar);
    }
    
    @Override
    protected double novAdd(U u, Tuple2od<U> itemValue, Tuple2od<U> compared)
    {
        U recomm = itemValue.v1;
        U del = compared.v1;

        int userComm = communities.getCommunity(u);
        int recommComm = communities.getCommunity(recomm);
        int delComm = communities.getCommunity(del);

        GiniIndex gini = new GiniIndex();
        
        if(userComm == recommComm)
        {
            return -1.0;
        }

        // No edge is added in terms of this metric.
        /*if(userComm == recommComm)
        {
            return this.globalvalue;
        }*/
        
        int newIdx = findIndex(userComm, recommComm);
        
        DoubleStream stream = IntStream.range(0, matrix.size()).mapToDouble(index -> 
        {
            if(index == newIdx)
            {
                return matrix.get(index) + 1.0;
            }
            else
            {
                return matrix.get(index);
            }
        });
        
        int numComms = communities.getNumCommunities();
        int numPairs = numComms*(numComms-1)/2;

        return 1.0 - gini.compute(stream.boxed(), true, numPairs, sum + 1.0);
    }
    
    @Override
    protected double novDelete(U u, Tuple2od<U> itemValue, Tuple2od<U> compared)
    {
        U recomm = itemValue.v1;
        U del = compared.v1;

        int userComm = communities.getCommunity(u);
        int recommComm = communities.getCommunity(recomm);
        int delComm = communities.getCommunity(del);

        GiniIndex gini = new GiniIndex();
        

        // No edge is added in terms of this metric.
        if(userComm == delComm)
        {
            return this.globalvalue;
        }
        
        int delIdx = findIndex(userComm, delComm);
        
        DoubleStream stream = IntStream.range(0, matrix.size()).mapToDouble(index -> 
        {
            if(index == delIdx)
            {
                return matrix.get(index) - 1.0;
            }
            else
            {
                return matrix.get(index);
            }
        });
        
        int numComms = communities.getNumCommunities();
        int numPairs = numComms*(numComms-1)/2;

        return 1.0 - gini.compute(stream.boxed(), true, numPairs, sum - 1.0);
    }
            

    @Override
    protected void innerUpdate(U user, Tuple2od<U> updated, Tuple2od<U> old)
    {
        U recomm = updated.v1;
        U del = old.v1;

        int userComm = communities.getCommunity(user);
        int recommComm = communities.getCommunity(recomm);
        int delComm = communities.getCommunity(del);
        GiniIndex gini = new GiniIndex();

        int newIdx = findIndex(userComm, recommComm);
        int oldIdx = findIndex(userComm, delComm);

        double addDel;
        double addNew;
        if(communityGraph.isDirected())
        {
            addDel = (userComm == delComm) ? 0.0 : -1.0;
            addNew = (userComm == recommComm) ? 0.0 : 1.0;
        }
        else if(recs.get(del).contains(user) && recs.get(recomm).contains(user))
        {
            // Do nothing
            addDel = 0.0;
            addNew = 0.0;
        }
        else if(recs.get(del).contains(user))
        {
            addDel = 0.0;
            addNew = (userComm == recommComm) ? 0.0 : 1.0;
        }
        else if(recs.get(recomm).contains(user))
        {
            addDel = (userComm == delComm) ? 0.0 : -1.0;
            addNew = 0.0;
        }
        else
        {
            addDel = (userComm == delComm) ? 0.0 : -1.0;
            addNew = (userComm == recommComm) ? 0.0 : 1.0;
        }
        
        double sumVar = addDel + addNew;
        
        this.sum += sumVar;
        if(newIdx != -1)
            matrix.set(newIdx, matrix.get(newIdx) + addNew);
        if(oldIdx != -1)
            matrix.set(oldIdx, matrix.get(oldIdx) + addDel);
        
        int numComm = this.communities.getNumCommunities();
        int numPairs = graph.isDirected() ? numComm*(numComm-1) : numComm*(numComm-1)/2;
        this.globalvalue = 1.0 - gini.compute(matrix, true, numPairs, this.sum);
    }

    @Override
    protected void computeGlobalValue() 
    {
        GiniIndex gini = new GiniIndex();

        super.computeGlobalValue();
        long vertexcount = communityGraph.getVertexCount();
        sum = communityGraph.getEdgeCount();
        
        double autoloops = 0.0;
        for(int i = 0; i < vertexcount; ++i)
        {
            autoloops += this.communityGraph.getNumEdges(i, i) + 0.0;
        }
        
        this.sum -= autoloops;
        
        if(communityGraph.isDirected())
        {
            for(int i = 0; i < vertexcount; ++i)
            {
                for(int j = 0; j < vertexcount; ++j)
                {
                    if(i!=j)
                        matrix.add(communityGraph.getNumEdges(i, j)+0.0);
                }
            }
            this.globalvalue = 1.0 - gini.compute(matrix, true, communities.getNumCommunities()*(communities.getNumCommunities()-1), this.sum);
        }
        else
        {
            for(int i = 0; i < vertexcount; ++i)
                for(int j = i+1; j < vertexcount; ++j)
                    matrix.add(communityGraph.getNumEdges(i, j)+0.0);
            this.globalvalue = 1.0 - gini.compute(matrix, true, communities.getNumCommunities()*(communities.getNumCommunities()-1)/2, this.sum);
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
            return -1;
        }
        else if(communityGraph.isDirected()) // In case the graph is directed.
        {
            return userComm*(communities.getNumCommunities()-1) + recommComm - (userComm > recommComm ? 0 : 1);
        }
        else // In case the graph is undirected.
        {
            int auxNewIdx = 0;
            for(int i = 0; i < min(userComm, recommComm); ++i)
            {
                auxNewIdx += communities.getNumCommunities() - i - 1;
            }
            auxNewIdx += max(userComm, recommComm) - min(userComm, recommComm) - 1;
            return auxNewIdx;
        }
    }
    
}
