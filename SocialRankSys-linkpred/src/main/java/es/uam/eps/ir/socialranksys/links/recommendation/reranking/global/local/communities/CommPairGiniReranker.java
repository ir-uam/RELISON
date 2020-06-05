/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.local.communities;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.utils.indexes.GiniIndex;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * Reranks a recommendation by improving the Gini Index of the degree of the 
 * different communities in a community graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public class CommPairGiniReranker<U> extends CommunityReranker<U> 
{
    /**
     * Number of edges between communities
     */
    private final List<Double> matrix;
    /**
     * Total number of edges between communities.
     */
    private double sum;
    /**
     * Constructor.
     * @param lambda Establishes the trait-off between the value of the Gini Index and the original score
     * @param cutoff The number of recommended items for each user.
     * @param norm true if the score and the Gini Index value have to be normalized.
     * @param graph The user graph.
     * @param communities A relation between communities and users in the graph.
     * @param rank Indicates if the normalization is by ranking (true) or by score (false)
     */
    public CommPairGiniReranker(double lambda, int cutoff, boolean norm, Graph<U> graph, Communities<U> communities, boolean rank)
    {
        super(lambda, cutoff, norm, rank, graph, communities);
        
        matrix = new ArrayList<>();
        long vertexcount = communityGraph.getVertexCount();
        
        if(communityGraph.isDirected())
        {
            for(int i = 0; i < communityGraph.getVertexCount(); ++i)
            {
                for(int j = 0; j < communityGraph.getVertexCount(); ++j)
                {
                    if(i != j )
                        matrix.add(communityGraph.getNumEdges(i, j)+0.0);
                }
            }
        }
        else
        {
            for(int i = 0; i < communityGraph.getVertexCount(); ++i)
                for(int j = 0; j < i; ++j)
                    matrix.add(communityGraph.getNumEdges(i, j)+0.0);
        }

        sum = communityGraph.getEdgeCount();
    }

    @Override
    protected double nov(U u, Tuple2od<U> tpld)
    {
        U recomm = tpld.v1;

        int userComm = communities.getCommunity(u);
        int recommComm = communities.getCommunity(recomm);

        GiniIndex gini = new GiniIndex();
        if(communityGraph.isDirected())
        {
            int idx = userComm*communities.getNumCommunities() + recommComm - (userComm + 1);
            DoubleStream stream = IntStream.range(0, matrix.size()).mapToDouble(index -> (index == idx ? (matrix.get(index) + 1) : matrix.get(index)));
            double giniValue = 1.0 - gini.compute(stream.boxed(), true, communities.getNumCommunities()*(communities.getNumCommunities()-1), this.sum+1);
            return 1.0-giniValue;
        }
        else
        {

            int idx = Math.min(userComm, recommComm)*communities.getNumCommunities() + Math.max(userComm, recommComm) - (Math.min(userComm,recommComm) + 1);
            double value = this.matrix.get(idx);
            this.matrix.set(idx, value+1);
            double giniValue = 1.0 - gini.compute(matrix, true, communities.getNumCommunities()*(communities.getNumCommunities()-1)/2, this.sum+1);
            this.matrix.set(idx, value);
            return 1.0-giniValue;
        }
    }
    
    @Override
    protected void update(U user, Tuple2od<U> tpld)
    {
        U recomm = tpld.v1;

        int userComm = communities.getCommunity(user);
        int recommComm = communities.getCommunity(recomm);

        matrix.set( userComm*communities.getNumCommunities()-(userComm+1), 
                    matrix.get(userComm*communities.getNumCommunities()-userComm-1)+1);
        if(communityGraph.isDirected())
        {
            int idx = userComm*communities.getNumCommunities() + recommComm - (userComm + 1);
            matrix.set(idx, matrix.get(idx)+1);
        }
        else
        {
            int idx = Math.min(userComm, recommComm)*communities.getNumCommunities() + Math.max(userComm, recommComm) - (Math.min(userComm,recommComm) + 1);
            matrix.set(idx, matrix.get(idx)+1);
        }
        sum++;
    }    

    @Override
    protected void update(Recommendation<U, U> reranked) {
    }
}
