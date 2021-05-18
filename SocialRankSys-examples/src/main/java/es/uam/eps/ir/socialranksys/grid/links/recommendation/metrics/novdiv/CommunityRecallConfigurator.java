/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.metrics.novdiv;

import es.uam.eps.ir.ranksys.core.feature.FeatureData;
import es.uam.eps.ir.ranksys.core.preference.PreferenceData;
import es.uam.eps.ir.ranksys.diversity.other.metrics.SRecall;
import es.uam.eps.ir.ranksys.metrics.SystemMetric;
import es.uam.eps.ir.ranksys.metrics.basic.AverageRecommendationMetric;
import es.uam.eps.ir.ranksys.metrics.rel.NoRelevanceModel;
import es.uam.eps.ir.ranksys.metrics.rel.RelevanceModel;
import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.metrics.RecommMetricConfigurator;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.metrics.RecommendationMetricFunction;
import es.uam.eps.ir.socialranksys.links.recommendation.features.CommunityFeatureData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.links.recommendation.metrics.RecommMetricIdentifiers.CRECALL;

/**
 * Grid search for configuring the community recall of the recommendations.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see SRecall
 */
public class CommunityRecallConfigurator<U, F> implements RecommMetricConfigurator<U, F>
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

        RelevanceModel<U,U> relModel = new NoRelevanceModel<>();
        cutoffs.forEach(cutoff ->
        {
            RecommendationMetricFunction<U,F> function = (trainGraph, testGraph, trainData, testData, featureData, comms) ->
            {
                FeatureData<U, Integer, Double> commData = CommunityFeatureData.load(comms);
                int numUsers = Long.valueOf(trainGraph.getVertexCount()).intValue();
                return new AverageRecommendationMetric<>(new SRecall<>(commData, cutoff, relModel), numUsers);
            };
            metrics.put(CRECALL + "@" + cutoff,  function);
        });

        return metrics;
    }

    @Override
    public Map<String, Supplier<SystemMetric<U, U>>> grid(Grid grid, Graph<U> trainGraph, Graph<U> testGraph, PreferenceData<U, U> trainData, PreferenceData<U,U> testData, FeatureData<U,F,Double> featureData, Communities<U> comms)
    {
        Map<String, Supplier<SystemMetric<U,U>>> metrics = new HashMap<>();
        List<Integer> cutoffs = grid.getIntegerValues(CUTOFF);
        FeatureData<U, Integer, Double> commData = CommunityFeatureData.load(comms);
        int numUsers = Long.valueOf(trainGraph.getVertexCount()).intValue();
        RelevanceModel<U,U> relModel = new NoRelevanceModel<>();

        cutoffs.forEach(cutoff ->
            metrics.put(CRECALL + "@" + cutoff, () -> new AverageRecommendationMetric<>(new SRecall<>(commData, cutoff, relModel), numUsers))
        );

        return metrics;
    }
}
