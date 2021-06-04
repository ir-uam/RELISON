/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.reranking.local.communities;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.relison.community.Communities;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.multigraph.MultiGraph;
import es.uam.eps.ir.relison.links.recommendation.reranking.normalizer.Normalizer;
import es.uam.eps.ir.relison.utils.indexes.GiniIndex;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * Reranks a recommendation by improving the Gini Index of the pairs of
 * different communities in a community graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public class InterCommunityEdgeGiniComplement<U> extends InterCommunityReranker<U>
{

    /**
     * the number of edges between each pair of communities.
     */
    private final List<Double> matrix;
    /**
     * The total number of edges between communities
     */
    private final double sum;

    /**
     * Constructor.
     * @param lambda        trade-off between the recommendation score and the novelty/diversity value.
     * @param cutoff        number of elements to take.
     * @param norm          the normalization strategy.
     * @param graph         the original graph.
     * @param communities   the relation between users and communities.
     */
    public InterCommunityEdgeGiniComplement(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> communities)
    {
        super(lambda, cutoff, norm, graph, communities);
        
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
    protected GreedyUserReranker<U, U> getUserReranker(Recommendation<U, U> r, int i)
    {
        return new CommPairGiniUserReranker(r, i, this.communityGraph, this.communities, this.matrix, this.sum);
    }
    
    /**
     * Class that reranks an individual recommendation using Community Pair Gini.
     */
    public class CommPairGiniUserReranker extends CommunityMetricUserReranker
    {
        /**
        * the number of edges between each pair of communities.
        */
        private final List<Double> matrix;
        /**
        * The total number of edges between communities
        */
        private double sum;

        /**
         * Constructor
         * @param recommendation    the recommendation to be reranked
         * @param maxLength         the maximum length of the reranked list.
         * @param communityGraph    the community graph.
         * @param communities       the communities
         * @param matrix            the number of edges between each pair of communities.
         * @param sum               the sum of the degrees.
         */
        public CommPairGiniUserReranker(Recommendation<U, U> recommendation, int maxLength, MultiGraph<Integer> communityGraph, Communities<U> communities, List<Double> matrix, double sum) {
            super(recommendation, maxLength, communityGraph, communities);
            this.matrix = matrix;
            this.sum = sum;
            
        }
            
        @Override
        protected double nov(Tuple2od<U> tpld)
        {
            U user = recommendation.getUser();
            U recomm = tpld.v1;
            
            int userComm = communities.getCommunity(user);
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
        protected void update(Tuple2od<U> tpld)
        {
            U user = recommendation.getUser();
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
    }
    
}
