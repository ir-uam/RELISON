/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.links.recommendation.metrics;

import es.uam.eps.ir.ranksys.core.feature.FeatureData;
import es.uam.eps.ir.ranksys.core.preference.PreferenceData;
import es.uam.eps.ir.ranksys.metrics.SystemMetric;
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.grid.Grid;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Class for configuring a given recommendation metric.
 *
 * @param <U> type of the users.
 * @param <F> type of the features.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface RecommMetricConfigurator<U, F>
{
    /**
     * Obtains the different recommendation algorithms to execute in a grid.
     *
     * @param grid The grid for the algorithm.
     *
     * @return a map containing the different recommendations.
     */
    Map<String, RecommendationMetricFunction<U,F>> grid(Grid grid);

    /**
     * Obtains the different recommendation algorithms to execute in a grid.
     *
     * @param grid          the grid containing the different parameters for the metric.
     * @param trainGraph        the training network.
     * @param testGraph         the test network.
     * @param trainData         the training preference data.
     * @param testData          the test preference data.
     * @param featureData       the feature data.
     * @param comms             the communities for the users in the network.
     *
     * @return a map containing the different recommendations.
     */
    Map<String, Supplier<SystemMetric<U, U>>> grid(Grid grid, Graph<U> trainGraph, Graph<U> testGraph, PreferenceData<U, U> trainData, PreferenceData<U,U> testData, FeatureData<U,F,Double> featureData, Communities<U> comms);
}
