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
import es.uam.eps.ir.ranksys.metrics.SystemMetric;
import es.uam.eps.ir.ranksys.novdiv.distance.CosineFeatureItemDistanceModel;
import es.uam.eps.ir.ranksys.novdiv.distance.ItemDistanceModel;
import es.uam.eps.ir.ranksys.novelty.unexp.PDItemNovelty;
import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.metrics.RecommMetricConfigurator;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.metrics.RecommendationMetricFunction;
import es.uam.eps.ir.socialranksys.links.recommendation.metrics.novdiv.Unexpectedness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.links.recommendation.metrics.RecommMetricIdentifiers.UNEXP;

/**
 * Grid search for configuring the unexpectedness of the recommendations.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see es.uam.eps.ir.socialranksys.links.recommendation.metrics.novdiv.Unexpectedness
 */
public class UnexpectednessConfigurator<U, F> implements RecommMetricConfigurator<U, F>
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
                new Unexpectedness<>(cutoff, new PDItemNovelty<>(true, trainData,  new CosineFeatureItemDistanceModel<>(featureData)));
            metrics.put(UNEXP + "@" + cutoff,  function);
        });

        return metrics;
    }

    @Override
    public Map<String, Supplier<SystemMetric<U, U>>> grid(Grid grid, Graph<U> trainGraph, Graph<U> testGraph, PreferenceData<U, U> trainData, PreferenceData<U,U> testData, FeatureData<U,F,Double> featureData, Communities<U> comms)
    {

        Map<String, Supplier<SystemMetric<U,U>>> metrics = new HashMap<>();
        List<Integer> cutoffs = grid.getIntegerValues(CUTOFF);

        ItemDistanceModel<U> itemDistanceModel = new CosineFeatureItemDistanceModel<>(featureData);
        cutoffs.forEach(cutoff ->
            metrics.put(UNEXP + "@" + cutoff, () -> new Unexpectedness<>(cutoff, new PDItemNovelty<>(true, trainData, itemDistanceModel)))
        );

        return metrics;
    }
}
