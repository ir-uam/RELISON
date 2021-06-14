/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.links.recommendation.rerankers;


import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.links.recommendation.reranking.global.GlobalReranker;
import es.uam.eps.ir.relison.links.recommendation.reranking.normalizer.Normalizer;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Interface for obtaining the different configurations of a given global reranking algorithm.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 */
public interface RerankerGridSearch<U>
{
    /**
     * Obtains the different reranking approaches given a grid.
     * @param grid      the parameter grid of the algorithm.
     * @param cutoff    the size of the definitive ranking.
     * @param norm      suppliers for the normalization scheme.
     * @param graph     the training graph.
     * @param comms     the community partition for the training graph.
     * @return a map containing suppliers for the different configurations of the reranking approach.
     */
    Map<String, Supplier<GlobalReranker<U,U>>> grid(Grid grid, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> comms);

    /**
     * Obtains the different reranking approaches given a grid.
     * @param grid the parameter grid for the algorithm.
     * @return a map containing suppliers for the different configurations of the reranking approach.
     */
    Map<String, GlobalRerankerFunction<U>> grid(Grid grid);
}
