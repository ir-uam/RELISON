/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.communities;

import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.graph.Graph;
import org.ranksys.core.util.tuples.Tuple2od;

/**
 * Reranker by the number of items on each community.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class GlobalInverseCommunitySizeReranker<U> extends GlobalCommunityReranker<U>
{

    /**
     * Constructor
     * @param lambda A trait-off between the original score and the metric value
     * @param cutoff The number of items to rerank
     * @param norm true if the original score and the metric value require optimization
     * @param graph The user graph
     * @param communities A relation between communities and users.
     */
    public GlobalInverseCommunitySizeReranker(double lambda, int cutoff, boolean norm, Graph<U> graph, Communities<U> communities) {
        super(lambda, cutoff, norm, graph, communities);
    }

    @Override
    protected double nov(U user, Tuple2od<U> item) {
        int comm = communities.getCommunity(item.v1);
            return 0.0 + graph.getVertexCount() - communities.getUsers(comm).count();
    }

    @Override
    protected void update(U user, Tuple2od<U> selectedItem)
    {
    }
}
