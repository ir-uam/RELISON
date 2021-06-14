/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.links.recommendation.metrics.accuracy;

import es.uam.eps.ir.ranksys.core.feature.FeatureData;
import es.uam.eps.ir.ranksys.core.preference.PreferenceData;
import es.uam.eps.ir.ranksys.metrics.SystemMetric;
import es.uam.eps.ir.ranksys.metrics.basic.AverageRecommendationMetric;
import es.uam.eps.ir.ranksys.metrics.rel.IdealRelevanceModel;
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.grid.links.recommendation.metrics.RecommMetricConfigurator;
import es.uam.eps.ir.relison.grid.links.recommendation.metrics.RecommendationMetricFunction;
import es.uam.eps.ir.relison.links.recommendation.metrics.accuracy.ContactRecommendationRelevanceModel;
import es.uam.eps.ir.relison.links.recommendation.metrics.accuracy.TRECAveragePrecision;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.relison.grid.links.recommendation.metrics.RecommMetricIdentifiers.MAP;

/**
 * Grid search generator for the mean average precision (MAP) metric.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see TRECAveragePrecision
 */
public class MAPConfigurator<U, F> implements RecommMetricConfigurator<U, F>
{
    /**
     * Identifier for the maximum number of recommended links to consider.
     */
    private final static String CUTOFF = "cutoff";

    @Override
    public Map<String, RecommendationMetricFunction<U,F>> grid(Grid grid)
    {
        Map<String, RecommendationMetricFunction<U,F>> metrics = new HashMap<>();
        List<Integer> cutoffs = grid.getIntegerValues(CUTOFF);


        cutoffs.forEach(cutoff ->
        {
            RecommendationMetricFunction<U,F> function = (trainGraph, testGraph, trainData, testData, featureData, comms) ->
            {
                IdealRelevanceModel<U,U> idealModel = new ContactRecommendationRelevanceModel<>(true, testGraph);
                return new AverageRecommendationMetric<>(new TRECAveragePrecision<>(cutoff, idealModel), testData.numUsersWithPreferences());
            };
            metrics.put(MAP + "@" + cutoff,  function);
        });

        return metrics;
    }

    @Override
    public Map<String, Supplier<SystemMetric<U, U>>> grid(Grid grid, Graph<U> trainGraph, Graph<U> testGraph, PreferenceData<U, U> trainData, PreferenceData<U,U> testData, FeatureData<U,F,Double> featureData, Communities<U> comms)
    {
        Map<String, Supplier<SystemMetric<U,U>>> metrics = new HashMap<>();
        List<Integer> cutoffs = grid.getIntegerValues(CUTOFF);

        IdealRelevanceModel<U,U> idealModel = new ContactRecommendationRelevanceModel<>(true, testGraph);
        cutoffs.forEach(cutoff ->
            metrics.put(MAP + "@" + cutoff, () -> new AverageRecommendationMetric<>(new TRECAveragePrecision<>(cutoff, idealModel), testData.numUsersWithPreferences()))
        );

        return metrics;
    }
}
