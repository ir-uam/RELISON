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
import es.uam.eps.ir.relison.links.linkprediction.metrics.LinkPredictionMetric;
import es.uam.eps.ir.relison.links.linkprediction.metrics.Precision;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.relison.grid.links.prediction.metrics.LinkPredictionMetricIdentifiers.P;

/**
 * Grid search generator for the precision metric.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see Precision
 */
public class PrecisionConfigurator<U, F> implements LinkPredictionMetricConfigurator<U, F>
{
    /**
     * Identifier for the cutoff of the link prediction ranking.
     */
    private final static String CUTOFF = "cutoff";
    /**
     * Identifier for the threshold of the link prediction ranking.
     */
    private final static String THRESHOLD = "threshold";

    @Override
    public Map<String, LinkPredictionMetricFunction<U,F>> grid(Grid grid)
    {
        Map<String, LinkPredictionMetricFunction<U,F>> metrics = new HashMap<>();

        List<Integer> cutoffs = grid.getIntegerValues(CUTOFF);
        List<Double> thresholds = grid.getDoubleValues(THRESHOLD);

        cutoffs.forEach(cutoff -> metrics.put(P + "@" + cutoff, ((trainGraph, testGraph, trainData, testData, featData, comms) -> new Precision<>(cutoff))));
        thresholds.forEach(threshold -> metrics.put(P + "_" + threshold, ((trainGraph, testGraph, trainData, testData, featData, comms) -> new Precision<>(threshold))));

        return metrics;
    }

    @Override
    public Map<String, Supplier<LinkPredictionMetric<U>>> grid(Grid grid, Graph<U> trainGraph, Graph<U> testGraph, PreferenceData<U, U> trainData, PreferenceData<U, U> testData, FeatureData<U, F, Double> featureData, Communities<U> comms)
    {
        Map<String, Supplier<LinkPredictionMetric<U>>> metrics = new HashMap<>();

        List<Integer> cutoffs = grid.getIntegerValues(CUTOFF);
        List<Double> thresholds = grid.getDoubleValues(THRESHOLD);

        cutoffs.forEach(cutoff -> metrics.put(P + "@" + cutoff, () -> new Precision<>(cutoff)));
        thresholds.forEach(threshold -> metrics.put(P + "_" + threshold, () -> new Precision<>(threshold)));

        return metrics;
    }
}
