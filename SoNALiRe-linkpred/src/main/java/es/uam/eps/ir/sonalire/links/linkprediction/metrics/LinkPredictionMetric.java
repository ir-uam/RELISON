/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.linkprediction.metrics;

import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.links.linkprediction.Prediction;
import es.uam.eps.ir.sonalire.utils.datatypes.Pair;

import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Interface for defining metrics for evaluating link prediction algorithms.
 * @param <U> type of the users.
 */
public interface LinkPredictionMetric<U>
{
    /**
     * Evaluates a link prediction algorithm. It considers that the prediction is applied over all pairs
     * of users in the network.
     *
     * @param graph a graph containing the set of links to predict.
     * @param pred  the prediction.
     * @return the value of the metric.
     */
    double evaluate(Graph<U> graph, Prediction<U> pred);

    /**
     * Evaluates a link prediction algorithm. It considers that the prediction is only applied for a subset
     * of pairs of users in the network: those who pass a given filter.
     * @param graph     a graph containing the set of links to predict.
     * @param pred      the prediction.
     * @param filter    the filter for the different pairs of users.
     * @return the value of the metric.
     */
    double evaluate(Graph<U> graph, Prediction<U> pred, Predicate<Pair<U>> filter);

    /**
     * Evaluates a link prediction algorithm. It considers that we only apply this over a given
     * set of links.
     * @param graph a graph containing the set of links to predict.
     * @param pred  the prediction.
     * @param pairs the set of users to apply the recommendation to.
     * @return the value of the metric.
     */
    double evaluate(Graph<U> graph, Prediction<U> pred, Stream<Pair<U>> pairs);


}
