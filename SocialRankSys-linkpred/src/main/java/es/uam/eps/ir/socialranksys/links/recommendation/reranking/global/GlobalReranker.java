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
 * Re-ranker. Changes the position of items in a list of recommendations to optimize global criteria
 * other than relevance. Previous added recommendations are considered when computing the global value
 * we want to optimize.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the items.
 */
public interface GlobalReranker<U, I> 
{
    /**
     * Re-ranks a set of recommendations
     * @param recommendation A stream containing all the recommendations to rerank
     * @param maxLength Maximum length of the re-ranking for each user
     * @return a stream of recommendations containing the re-rankings of the inputs
     */
    Stream<Recommendation<U,I>> rerankRecommendations(Stream<Recommendation<U,I>> recommendation, int maxLength);
}
