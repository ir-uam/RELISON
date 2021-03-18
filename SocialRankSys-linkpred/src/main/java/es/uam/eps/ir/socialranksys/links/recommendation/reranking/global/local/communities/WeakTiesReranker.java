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
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.normalizer.Normalizer;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.function.Supplier;

/**
 * Reranker that promotes the number of links between different communities.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class WeakTiesReranker<U> extends CompleteCommunityReranker<U>
{

    /**
     * Constructor.
     * @param lambda        trade-off between the recommendation score and the novelty/diversity value.
     * @param cutoff        number of elements to take.
     * @param norm          the normalization strategy.
     * @param graph         the original graph.
     * @param communities   the relation between users and communities.
     */
    public WeakTiesReranker(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> communities, boolean rank) {
        super(lambda, cutoff, norm, graph, communities, false);
    }

    @Override
    protected double nov(U u, Tuple2od<U> itemValue)
    {
        return (this.communities.getCommunity(u) == this.communities.getCommunity(itemValue.v1)) ? 0.0 : 1.0;
    }

    @Override
    protected void innerUpdate(U user, Tuple2od<U> bestItemValue)
    {
        
    }



    @Override
    protected void update(Recommendation<U, U> reranked)
    {

    }
    
}
