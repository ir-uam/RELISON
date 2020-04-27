/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.data.letor.sampling;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Obtains a sample of users starting from a single user.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public interface IndividualSampler<U> 
{
    /**
     * Given a user, obtains the sample.
     * @param u the parameter.
     * @param filter a filter for the sample
     * @return the set of sampled users, with their relevance / weight.
     */
    Set<U> sampleUsers(U u, Predicate<U> filter);
}
