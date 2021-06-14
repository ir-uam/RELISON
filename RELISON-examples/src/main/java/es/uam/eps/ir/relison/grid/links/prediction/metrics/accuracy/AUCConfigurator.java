/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.links.prediction.metrics.accuracy;

import es.uam.eps.ir.ranksys.core.feature.FeatureData;
import es.uam.eps.ir.ranksys.core.preference.PreferenceData;
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.grid.links.prediction.metrics.LinkPredictionMetricConfigurator;
import es.uam.eps.ir.relison.grid.links.prediction.metrics.LinkPredictionMetricFunction;
import es.uam.eps.ir.relison.grid.links.prediction.metrics.LinkPredictionMetricIdentifiers;
import es.uam.eps.ir.relison.links.linkprediction.metrics.AUC;
import es.uam.eps.ir.relison.links.linkprediction.metrics.LinkPredictionMetric;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;


/**
 * Grid search generator for area under the ROC curve.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see AUC
 */
public class AUCConfigurator<U, F> implements LinkPredictionMetricConfigurator<U, F>
{
    @Override
    public Map<String, LinkPredictionMetricFunction<U,F>> grid(Grid grid)
    {
        Map<String, LinkPredictionMetricFunction<U,F>> metrics = new HashMap<>();
        metrics.put(LinkPredictionMetricIdentifiers.AUC, (trainGraph, testGraph, trainData, testData, featData, comms) -> new AUC<>());
        return metrics;
    }

    @Override
    public Map<String, Supplier<LinkPredictionMetric<U>>> grid(Grid grid, Graph<U> trainGraph, Graph<U> testGraph, PreferenceData<U, U> trainData, PreferenceData<U, U> testData, FeatureData<U, F, Double> featureData, Communities<U> comms)
    {
        Map<String, Supplier<LinkPredictionMetric<U>>> metrics = new HashMap<>();
        metrics.put(LinkPredictionMetricIdentifiers.AUC, AUC::new);

        return metrics;
    }
}
