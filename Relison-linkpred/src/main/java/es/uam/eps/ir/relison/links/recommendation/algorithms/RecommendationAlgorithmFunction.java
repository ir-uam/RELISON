/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.algorithms;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.relison.graph.fast.FastGraph;

/**
 * Functions for retrieving trained recommendation algorithms.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
@FunctionalInterface
public interface RecommendationAlgorithmFunction<U>
{
    /**
     * Given a graph, and the preference data, obtains a trained algorithm.
     *
     * @param graph    the graph.
     * @param prefData the preference data.
     *
     * @return the trained recommender.
     */
    Recommender<U, U> apply(FastGraph<U> graph, FastPreferenceData<U, U> prefData);

    /**
     * Indicates if the algorithm version to use is weighted or not.
     * @return true if the algorithm version is weighted, false otherwise.
     */
    default boolean isWeighted() { return false;}
}
