/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global;

import es.uam.eps.ir.ranksys.core.Recommendation;

import java.util.stream.Stream;

/**
 * Interface for defining reranking strategies which change the position of items in recommendation lists
 * to optimize global properties of the system beyond relevance.
 *
 * Global rerankers go user by user, and consider the recommendations made to the previously visited users
 * when optimizing the global value.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the items.
 */
public interface GlobalReranker<U, I> 
{
    /**
     * Reranks a set of recommendations.
     * @param recommendation a stream containing all the recommendations to rerank.
     * @param maxLength      maximum length of the definitive ranking for each user.
     * @return a stream of recommendations containing the definitive rankings.
     */
    Stream<Recommendation<U,I>> rerankRecommendations(Stream<Recommendation<U,I>> recommendation, int maxLength);
}
