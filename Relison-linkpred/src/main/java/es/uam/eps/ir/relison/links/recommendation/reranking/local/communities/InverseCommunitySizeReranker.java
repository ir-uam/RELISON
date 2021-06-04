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
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.function.Supplier;

/**
 * Implementation of a reranker which promotes recommending links in small communities.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 */
public class InverseCommunitySizeReranker<U> extends InterCommunityReranker<U>
{

    /**
     * Constructor.
     * @param lambda        param that establishes a balance between the score and the novelty/diversity value.
     * @param cutoff        number of elements to take.
     * @param norm          the normalization function.
     * @param graph         the graph.
     * @param communities   the relation between users and communities.
     */
    public InverseCommunitySizeReranker(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> communities) {
        super(lambda, cutoff, norm, graph, communities);
    }

    @Override
    protected GreedyUserReranker<U, U> getUserReranker(Recommendation<U, U> r, int i) {
        return new InverseCommunityPopularityUserReranker(r, i, this.communityGraph, this.communities, this.graph.getVertexCount());
    }
    
    /**
     * Class that reranks an individual recommendation using the inverse community size.
     */
    protected class InverseCommunityPopularityUserReranker extends CommunityMetricUserReranker
    {

        /**
         * The number of users.
         */
        private final long numUsers;
        
        /**
         * Constructor
         * @param recommendation    the recommendation to be reranked.
         * @param maxLength         maximum length of the reranked ranking.
         * @param communityGraph    the community graph.
         * @param communities       the set of communities.
         * @param numUsers          the number of users.
         */
        public InverseCommunityPopularityUserReranker(Recommendation<U, U> recommendation, int maxLength, MultiGraph<Integer> communityGraph, Communities<U> communities, long numUsers) {
            super(recommendation, maxLength, communityGraph, communities);
            this.numUsers = numUsers;
        }

        @Override
        protected double nov(Tuple2od<U> tpld)
        {
            int comm = communities.getCommunity(tpld.v1);
            return 0.0 + this.numUsers - communities.getUsers(comm).count();
        }

        @Override
        protected void update(Tuple2od<U> tpld)
        {
        }

    }
}
