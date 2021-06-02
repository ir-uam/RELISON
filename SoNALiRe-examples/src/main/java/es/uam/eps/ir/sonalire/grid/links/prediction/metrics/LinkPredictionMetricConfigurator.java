/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.links.prediction.metrics;

import es.uam.eps.ir.ranksys.core.feature.FeatureData;
import es.uam.eps.ir.ranksys.core.preference.PreferenceData;
import es.uam.eps.ir.sonalire.community.Communities;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.grid.Grid;
import es.uam.eps.ir.sonalire.links.linkprediction.metrics.LinkPredictionMetric;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Class for configuring a given link prediction metric.
 *
 * @param <U> type of the users.
 * @param <F> type of the features.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface LinkPredictionMetricConfigurator<U, F>
{
    /**
     * Obtains the different link prediction metrics to compute in a grid.
     *
     * @param grid the grid for the link prediction metric.
     *
     * @return a map containing the different link prediction metrics.
     */
    Map<String, LinkPredictionMetricFunction<U,F>> grid(Grid grid);

    /**
     * Obtains the different link prediction metrics to compute in a grid.
     *
     * @param grid              the grid containing the different parameters for the metric.
     * @param trainGraph        the training network.
     * @param testGraph         the test network.
     * @param trainData         the training preference data.
     * @param testData          the test preference data.
     * @param featureData       the feature data.
     * @param comms             the communities for the users in the network.
     *
     * @return a map containing the different link prediction metrics.
     */
    Map<String, Supplier<LinkPredictionMetric<U>>> grid(Grid grid, Graph<U> trainGraph, Graph<U> testGraph, PreferenceData<U, U> trainData, PreferenceData<U,U> testData, FeatureData<U,F,Double> featureData, Communities<U> comms);
}
