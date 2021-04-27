/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.knn.similarities;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.nn.sim.Similarity;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.grid.Grid;

import java.util.Map;
import java.util.function.Supplier;


/**
 * Class for performing the grid search for a given similarity between users.
 *
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface SimilarityGridSearch<U>
{
    /**
     * Obtains the different user to user similarities to execute in a grid.
     *
     * @param grid the grid for the similarity
     *
     * @return a map containing the different user to user similarities.
     */
    Map<String, SimilarityFunction<U>> grid(Grid grid);

    /**
     * Obtains the different user to user similarities to execute in a grid.
     *
     * @param grid     the grid for the algorithm.
     * @param graph    the training graph.
     * @param prefData the preference training data.
     *
     * @return a map containing the different user to user similarities.
     */
    Map<String, Supplier<Similarity>> grid(Grid grid, FastGraph<U> graph, FastPreferenceData<U, U> prefData);
}
