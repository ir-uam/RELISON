/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.globalranking.communities;

import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.normalizer.Normalizer;
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
public class InverseCommunitySize<U> extends InterCommunityReranker<U>
{
    /**
     * Constructor.
     * @param lambda        trade-off between the recommendation score and the novelty/diversity value.
     * @param cutoff        number of elements to take.
     * @param norm          the normalization strategy.
     * @param graph         the original graph.
     * @param communities   the relation between users and communities.
     */
    public InverseCommunitySize(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> communities)
    {
        super(lambda, cutoff, norm, graph, communities);
    }

    @Override
    protected double nov(U user, Tuple2od<U> item)
    {
        int comm = communities.getCommunity(item.v1);
        return 0.0 + graph.getVertexCount() - communities.getUsers(comm).count();
    }

    @Override
    protected void update(U user, Tuple2od<U> selectedItem)
    {
    }
}
