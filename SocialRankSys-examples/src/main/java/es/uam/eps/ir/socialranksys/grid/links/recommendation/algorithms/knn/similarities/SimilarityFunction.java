/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.knn.similarities;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.nn.sim.Similarity;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;


/**
 * Function for retrieving a similarity object.
 * @author Javier Sanz-Cruzado
 * @param <U> Type of the users
 */
@FunctionalInterface
public interface SimilarityFunction<U> 
{
    /**
     * Obtains a similarity, given a graph and preference data.
     * @param graph the graph.
     * @param prefData the preference data.
     * @return the corresponding similarity.
     */
     Similarity apply(FastGraph<U> graph, FastPreferenceData<U, U> prefData);
}
