/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.swap.comm.gini.edge;

import com.google.common.util.concurrent.AtomicDouble;
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
 * between communities. Both edges between different communities and links inside of 
 * communities are considered, but links between communities are stored in a different
 * group when computing Gini coefficient.
 * 
 * This algorithm also looks promoting the existence of inter-link communities, so, when we add
 * new links, the reranking score will be -1.0 if the new link goes to the same community than 
 * the user whose recommendation we are reranking, or the variation in Gini in other case.
 * 
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public class SemiCompleteCommunityOuterEdgeGiniReranker<U> extends CommunityReranker<U>
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
     * Constructor.
     * @param lambda Establishes the trait-off between the value of the Gini Index and the original score
     * @param cutoff The number of recommended items for each user.
     * @param norm true if the score and the Gini Index value have to be normalized.
     * @param rank true if the normalization is by ranking position, false if it is by score
     * @param graph The user graph.
     * @param communities A relation between communities and users in the graph.
     * @param autoloops true if autoloops are allowed, false if they are not.
     */
    public SemiCompleteCommunityOuterEdgeGiniReranker(double lambda, int cutoff, boolean norm, boolean rank, Graph<U> graph, Communities<U> communities, boolean autoloops)
    {
        super(lambda, cutoff, norm, rank,  graph, communities, autoloops);
        
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

        int numComm = communities.getNumCommunities();
        int userComm = communities.getCommunity(u);
        int recommComm = communities.getCommunity(recomm);
        int delComm = communities.getCommunity(del);

        if(userComm == recommComm)
            return -1;
        
        GiniIndex gini = new GiniIndex();
        
        int newIdx = findIndex(userComm, recommComm);
        int delIdx = findIndex(userComm, delComm);
        
        DoubleStream stream = IntStream.range(0, matrix.size()).mapToDouble(index -> 
        {
            double value = matrix.get(index);

            if(index == newIdx)
            {
                value += 1.0;
            }

            if(index == delIdx)
            {
                value -= 1.0;
            }

            return value;
        });
        
        int numPairs = this.graph.isDirected() ? (numComm*(numComm - 1) + 1) : (numComm*(numComm - 1)/2 + 1);

        return 1.0 - gini.compute(stream.boxed(), true, numPairs, this.sum);
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

        if(userComm == recommComm)
            return -1;

        GiniIndex gini = new GiniIndex();
        
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
        
        int numPairs = this.graph.isDirected() ? (numComm*(numComm - 1) + 1) : (numComm*(numComm - 1)/2 + 1);

        return 1.0 - gini.compute(stream.boxed(), true, numPairs, this.sum + 1.0);
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

        AtomicDouble atom = new AtomicDouble();
        atom.set(this.sum);
        GiniIndex gini = new GiniIndex();
        
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
        
        int numPairs = this.graph.isDirected() ? (numComm*(numComm - 1) + 1) : (numComm*(numComm - 1)/2 + 1);

        return 1.0 - gini.compute(stream.boxed(), true, numPairs, this.sum - 1.0);
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
            for(int i = 0; i < vertexcount; ++i)
            {
                for(int j = 0; j < vertexcount; ++j)
                {
                    if(i != j)
                        matrix.add(communityGraph.getNumEdges(i, j)+0.0);
                }
            }
            
            double autoloops = 0.0;
            for(int i = 0; i < vertexcount; ++i)
            {
                autoloops += communityGraph.getNumEdges(i, i);
            }
            matrix.add(autoloops);
            
            sum = this.graph.getEdgeCount();
            this.globalvalue = 1.0 - gini.compute(matrix, true, communities.getNumCommunities()*(communities.getNumCommunities()-1)+1, this.sum);
        }
        else
        {
            for(int i = 0; i < vertexcount; ++i)
                for(int j = i+1; j < vertexcount; ++j)
                    matrix.add(communityGraph.getNumEdges(i, j)+0.0);
            double autoloops = 0.0;
            for(int i = 0; i < vertexcount; ++i)
            {
                autoloops += communityGraph.getNumEdges(i, i);
            }
            matrix.add(autoloops);
            this.globalvalue = 1.0 - gini.compute(matrix, true, communities.getNumCommunities()*(communities.getNumCommunities()-1)/2+1, this.sum);
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
}
