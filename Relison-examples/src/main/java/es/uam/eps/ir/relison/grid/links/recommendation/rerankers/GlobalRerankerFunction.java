/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.links.recommendation.rerankers;

import es.uam.eps.ir.relison.community.Communities;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.links.recommendation.reranking.global.GlobalReranker;
import es.uam.eps.ir.relison.links.recommendation.reranking.normalizer.Normalizer;

import java.util.function.Supplier;

/**
 * Functions for retrieving reranking algorithms.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
@FunctionalInterface
public interface GlobalRerankerFunction<U>
{
    /**
     * Given a graph, and the preference data, obtains a trained algorithm.
     *
     * @param cutoff   the maximum number of elements in the original recommendation to take.
     * @param norm     the normalization scheme.
     * @param graph    the training graph.
     * @param comms    the communities for the training graph.
     *
     * @return the global reranking algorithm.
     */
    GlobalReranker<U, U> apply(int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> comms);
}
