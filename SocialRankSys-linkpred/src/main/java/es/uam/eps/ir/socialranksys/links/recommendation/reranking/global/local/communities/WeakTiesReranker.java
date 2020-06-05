/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
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
 * Global local reranker for increasing the number of weak ties in the graph, thus
 * reducing the modularity.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public class WeakTiesReranker<U> extends CommunityReranker<U>
{

    /**
     * Constructor.
     * @param lambda Establishes the trade-off between the value of the Gini Index and the original score
     * @param cutoff The number of recommended items for each user.
     * @param norm true if the score and the Gini Index value have to be normalized.
     * @param rank true if rank normalization, false if min-max normalization
     * @param graph The user graph.
     * @param communities A relation between communities and users in the graph.
     */
    public WeakTiesReranker(double lambda, int cutoff, boolean norm, boolean rank, Graph<U> graph, Communities<U> communities) {
        super(lambda, cutoff, norm, rank, graph, communities);
    }
    
    /**
     * Constructor.
     * @param lambda Establishes the trade-off between the value of the Gini Index and the original score
     * @param cutoff The number of recommended items for each user.
     * @param norm true if the score and the Gini Index value have to be normalized.
     * @param graph The user graph.
     * @param communities A relation between communities and users in the graph.
     */
    public WeakTiesReranker(double lambda, int cutoff, boolean norm, Graph<U> graph, Communities<U> communities) 
    {
        super(lambda, cutoff, norm, true, graph, communities);
    }    

    @Override
    protected double nov(U u, Tuple2od<U> itemValue)
    {
        return (this.communities.getCommunity(u) == this.communities.getCommunity(itemValue.v1)) ? 0.0 : 1.0;
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
