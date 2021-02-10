/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
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
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 *
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
