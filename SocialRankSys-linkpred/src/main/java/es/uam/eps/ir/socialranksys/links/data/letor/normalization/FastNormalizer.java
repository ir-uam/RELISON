/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.data.letor.normalization;

import es.uam.eps.ir.ranksys.fast.FastRecommendation;

/**
 * Fast interface for normalizing the results of a recommendation.
 * @author Javier Sanz-Cruzado
 * @param <U> Type of the users.
 * @param <I> Type of the items.
 */
public interface FastNormalizer<U,I> extends Normalizer<U,I> 
{
    /**
     * Normalizes the recommendation.
     * @param rec the recommendation.
     * @return the normalized recommendation.
     */
    FastRecommendation normalize(FastRecommendation rec);
}
