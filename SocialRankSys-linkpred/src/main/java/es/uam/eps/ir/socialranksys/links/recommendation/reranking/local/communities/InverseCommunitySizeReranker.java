/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.local.communities;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.multigraph.MultiGraph;
import org.ranksys.core.util.tuples.Tuple2od;

/**
 * Reranker by the number of items on each community.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class InverseCommunitySizeReranker<U> extends CommunityReranker<U>
{

    /**
     * Constructor
     * @param lambda trade-off between the original ranking and the reranked one.
     * @param cutoff maximum length of the reranked ranking.
     * @param norm true if the original scores and reranked scores have to be normalized.
     * @param graph the graph.
     * @param communities the different communities.
     */
    public InverseCommunitySizeReranker(double lambda, int cutoff, boolean norm, Graph<U> graph, Communities<U> communities) {
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
         * @param recommendation the recommendation to be reranked.
         * @param maxLength maximum length of the reranked ranking.
         * @param communityGraph the community graph.
         * @param communities the set of communities.
         * @param numUsers the number of users.
         */
        public InverseCommunityPopularityUserReranker(Recommendation<U, U> recommendation, int maxLength, MultiGraph<Integer> communityGraph, Communities<U> communities, long numUsers) {
            super(recommendation, maxLength, communityGraph, communities);
            this.numUsers = numUsers;
        }

        @Override
        protected double nov(Tuple2od<U> tpld) {
            int comm = communities.getCommunity(tpld.v1);
            return 0.0 + this.numUsers - communities.getUsers(comm).count();
        }

        @Override
        protected void update(Tuple2od<U> tpld) {
        }

    }
}
