/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.metrics;

import es.uam.eps.ir.ranksys.core.feature.FeatureData;
import es.uam.eps.ir.ranksys.core.preference.PreferenceData;
import es.uam.eps.ir.ranksys.metrics.SystemMetric;
import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.graph.Graph;

/**
 * Functions for retrieving a configured recommendation metric.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
@FunctionalInterface
public interface RecommendationMetricFunction<U,F>
{
    /**
     * Given a graph, and the preference data, obtains a trained algorithm.
     *
     * @param trainGraph        the training network.
     * @param testGraph         the test network.
     * @param trainData         the training preference data.
     * @param testData          the test preference data.
     * @param featData          the feature data.
     * @param comms             the communities for the users in the network.
     *
     * @return a recommendation metric to apply.
     */
    SystemMetric<U, U> apply(Graph<U> trainGraph, Graph<U> testGraph, PreferenceData<U,U> trainData, PreferenceData<U,U> testData, FeatureData<U,F,Double> featData, Communities<U> comms);

}