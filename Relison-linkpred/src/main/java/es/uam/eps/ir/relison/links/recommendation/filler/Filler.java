/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.filler;

import es.uam.eps.ir.ranksys.core.Recommendation;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Methods for classes that might be used to complete recommendation lists which
 * do not fill themselves due to coverage problems of the algorithm.
 *
 * @author Javier Sanz-Cruzado Puig (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> Type of the users.
 * @param <I> Type of the items.
 */
public interface Filler<U,I>
{
    /**
     * Obtains the filler list for a given user.
     * @param u the user.
     * @return a stream containing the filler list for the user.
     */
    Stream<I> fillerList(U u);
    
    /**
     * Given a recommendation, fills it with until it reaches the desired number of items (if possible).
     * If the cutoff is greater than the indicated one, the recommendation is trimmed.
     * @param rec       the recommendation.
     * @param cutoff    the cutoff.
     * @param pred      a predicate for filtering the possible recommendations.
     * @return the new recommendation if everything is OK, null otherwise.
     */
    Recommendation<U,I> fill(Recommendation<U, I> rec, int cutoff, Function<U, Predicate<I>> pred);

    /**
     * Number of elements filled.
     * @return the number of elements filled in the last recommendation.
     */
    int numFilled();
    /**
     * Number of total elements in the recommendation.
     * @return the number of total elements in the recommendation.
     */
    int numTotal();
    /**
     * Resets the filler.
     */
    void reset();
}
