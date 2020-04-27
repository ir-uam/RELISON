/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.data.letor.normalization;

import es.uam.eps.ir.ranksys.core.Recommendation;

import java.util.List;


/**
 * Interface for normalizing the results of a recommendation.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users.
 * @param <I> type of the items.
 */
public interface Normalizer<U,I>
{
    /**
     * Normalizes a recommendation.
     * @param rec the recommendation.
     * @return the normalized recommendation.
     */
    Recommendation<U,I> normalize(Recommendation<U, I> rec);

    /**
     * Normalizes a list of double values (might be unsorted)
     * @param list the list of values.
     * @return the normalized list of values.
     */
    List<Double> normalize(List<Double> list);
}
