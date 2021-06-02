/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.algorithms.knn.similarities;

import es.uam.eps.ir.ranksys.fast.index.FastUserIndex;
import es.uam.eps.ir.ranksys.nn.sim.Similarity;
import es.uam.eps.ir.ranksys.nn.user.sim.UserSimilarity;

/**
 * Similarity between users.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class SpecificUserSimilarity<U> extends UserSimilarity<U>
{
    public SpecificUserSimilarity(FastUserIndex<U> uIndex, Similarity sim)
    {
        super(uIndex, sim);
    }
}
