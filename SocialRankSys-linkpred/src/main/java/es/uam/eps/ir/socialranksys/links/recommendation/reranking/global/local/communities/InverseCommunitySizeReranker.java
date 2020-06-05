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
import org.ranksys.core.util.tuples.Tuple2od;

/**
 * Reranker by the number of items on each community.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class InverseCommunitySizeReranker<U> extends CommunityReranker<U>
{

    /**
     * Constructor.
     * @param lambda Establishes the trait-off between the value of the Gini Index and the original score
     * @param cutoff The number of recommended items for each user.
     * @param norm true if the score and the Gini Index value have to be normalized.
     * @param graph The user graph.
     * @param communities A relation between communities and users in the graph.
     * @param rank Indicates if the normalization is by ranking (true) or by score (false)
     */
    public InverseCommunitySizeReranker(double lambda, int cutoff, boolean norm, Graph<U> graph, Communities<U> communities, boolean rank) {
        super(lambda, cutoff, norm, rank, graph, communities);
    }

    @Override
    protected double nov(U u, Tuple2od<U> iv)
    {
        int comm = communities.getCommunity(iv.v1);
        return 0.0 + this.graph.getVertexCount() - communities.getUsers(comm).count();    
    }

    @Override
    protected void update(U user, Tuple2od<U> bestItemValue)
    {
    }

    @Override
    protected void update(Recommendation<U, U> reranked)
    {
    }
}
