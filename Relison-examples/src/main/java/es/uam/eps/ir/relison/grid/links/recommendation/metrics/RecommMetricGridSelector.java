/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.links.recommendation.metrics;

import es.uam.eps.ir.ranksys.core.feature.FeatureData;
import es.uam.eps.ir.ranksys.core.preference.PreferenceData;
import es.uam.eps.ir.ranksys.metrics.SystemMetric;
import es.uam.eps.ir.relison.community.Communities;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.grid.Configurations;
import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.grid.Parameters;
import es.uam.eps.ir.relison.grid.links.recommendation.metrics.accuracy.MAPConfigurator;
import es.uam.eps.ir.relison.grid.links.recommendation.metrics.accuracy.NDCGConfigurator;
import es.uam.eps.ir.relison.grid.links.recommendation.metrics.accuracy.PrecisionConfigurator;
import es.uam.eps.ir.relison.grid.links.recommendation.metrics.accuracy.RecallConfigurator;
import es.uam.eps.ir.relison.grid.links.recommendation.metrics.novdiv.*;
import es.uam.eps.ir.relison.utils.datatypes.Tuple2oo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.relison.grid.links.recommendation.metrics.RecommMetricIdentifiers.*;

/**
 * Class that translates from a grid to the different contact recommendation algorithms.
 *
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class RecommMetricGridSelector<U, F>
{
    /**
     * Constructor.
     */
    public RecommMetricGridSelector()
    {
    }

    /**
     * Given a grid of parameters, obtains a set of configured metrics.
     *
     * @param metric            the name of the metric.
     * @param grid              the parameter grid for the metric.
     * @param trainGraph        the training network.
     * @param testGraph         the test network.
     * @param trainData         the training preference data.
     * @param testData          the test preference data.
     * @param featData          the feature data.
     * @param comms             the communities for the users in the network.
     *
     * @return the suppliers for the different metric variants, indexed by name.
     */
    public Map<String, Supplier<SystemMetric<U, U>>> getMetrics(String metric, Grid grid, Graph<U> trainGraph, Graph<U> testGraph, PreferenceData<U,U> trainData, PreferenceData<U,U> testData, FeatureData<U,F,Double> featData, Communities<U> comms)
    {
        RecommMetricConfigurator<U,F> gridsearch = this.selectGridSearch(metric);
        if (gridsearch != null)
            return gridsearch.grid(grid, trainGraph, testGraph, trainData, testData, featData, comms);
        return null;
    }

    /**
     * Given a set of parameters, obtains a single metric.
     *
     * @param metric            the name of the metric.
     * @param params            the parameters of the metric.
     * @param trainGraph        the training network.
     * @param testGraph         the test network.
     * @param trainData         the training preference data.
     * @param testData          the test preference data.
     * @param featData          the feature data.
     * @param comms             the communities for the users in the network.
     *
     * @return a map containing the suppliers of the algorithms, ordered by name.
     */
    public Tuple2oo<String, Supplier<SystemMetric<U, U>>> getMetrics(String metric, Parameters params, Graph<U> trainGraph, Graph<U> testGraph, PreferenceData<U,U> trainData, PreferenceData<U,U> testData, FeatureData<U,F,Double> featData, Communities<U> comms)
    {
        RecommMetricConfigurator<U, F> gridsearch = this.selectGridSearch(metric);
        if (gridsearch != null)
        {
            Grid grid = params.toGrid();
            Map<String, Supplier<SystemMetric<U, U>>> map = this.getMetrics(metric, grid, trainGraph, testGraph, trainData, testData, featData, comms);
            if (map == null || map.isEmpty()) return null;

            List<String> metrics = new ArrayList<>(map.keySet());
            String name = metrics.get(0);
            Supplier<SystemMetric<U, U>> supplier = map.get(name);
            return new Tuple2oo<>(name, supplier);
        }

        return null;
    }

    /**
     * Given a list of configurations, obtains a set of metrics.
     *
     * @param metric            the name of the metric.
     * @param configs           the different configurations for the metric.
     * @param trainGraph        the training network.
     * @param testGraph         the test network.
     * @param trainData         the training preference data.
     * @param testData          the test preference data.
     * @param featData          the feature data.
     * @param comms             the communities for the users in the network.
     *
     * @return a map containing the suppliers of the algorithms, ordered by name.
     */
    public Map<String, Supplier<SystemMetric<U, U>>> getMetrics(String metric, Configurations configs, Graph<U> trainGraph, Graph<U> testGraph, PreferenceData<U,U> trainData, PreferenceData<U,U> testData, FeatureData<U,F,Double> featData, Communities<U> comms)
    {
        Map<String, Supplier<SystemMetric<U, U>>> metrics = new HashMap<>();
        RecommMetricConfigurator<U, F> gridSearch = this.selectGridSearch(metric);
        if (gridSearch != null)
        {
            for (Parameters params : configs.getConfigurations())
            {
                Grid grid = params.toGrid();
                Map<String, Supplier<SystemMetric<U, U>>> map = getMetrics(metric, grid, trainGraph, testGraph, trainData, testData, featData, comms);
                if (map == null || map.isEmpty()) return null;

                List<String> metr = new ArrayList<>(map.keySet());
                String name = metr.get(0);
                Supplier<SystemMetric<U, U>> supplier = map.get(name);
                metrics.put(name, supplier);
            }
        }

        return metrics;
    }

    /**
     * Given a grid of parameters, obtains the metric.
     *
     * @param metric    the name of the metric.
     * @param grid      the parameter grid for the metric.
     *
     * @return functions for obtaining for the different metric variants given the graph and preference data, indexed by name.
     */
    public Map<String, RecommendationMetricFunction<U, F>> getMetrics(String metric, Grid grid)
    {
        RecommMetricConfigurator<U, F> gridsearch = this.selectGridSearch(metric);
        if (gridsearch != null)
            return gridsearch.grid(grid);
        return null;
    }

    /**
     * Given a list of parameter configurations, obtains the metrics.
     *
     * @param metric    the name of the metric.
     * @param configs   configurations for the metric.
     *
     * @return functions for obtaining for the different metric variants given the graph and preference data, indexed by name.
     */
    public Map<String, RecommendationMetricFunction<U, F>> getMetrics(String metric, Configurations configs)
    {
        RecommMetricConfigurator<U, F> gridsearch = this.selectGridSearch(metric);
        Map<String, RecommendationMetricFunction<U,F>> metrics = new HashMap<>();
        if (gridsearch != null)
        {
            for (Parameters params : configs.getConfigurations())
            {
                Grid grid = params.toGrid();
                Map<String, RecommendationMetricFunction<U, F>> map = getMetrics(metric, grid);
                if (map == null || map.isEmpty()) return null;

                List<String> metricNames = new ArrayList<>(map.keySet());
                String name = metricNames.get(0);

                metrics.put(name, map.get(name));
            }

            return metrics;
        }

        return null;
    }

    /**
     * Obtains a single metric.
     *
     * @param metric the name of the metric.
     * @param params the parameters of the metric.
     *
     * @return a tuple containing the name and a function for obtaining the metric.
     */
    public Tuple2oo<String, RecommendationMetricFunction<U, F>> getMetrics(String metric, Parameters params)
    {
        RecommMetricConfigurator<U, F> gridsearch = this.selectGridSearch(metric);
        if (gridsearch != null)
        {
            Grid grid = params.toGrid();
            Map<String, RecommendationMetricFunction<U, F>> map = getMetrics(metric, grid);
            if (map == null || map.isEmpty()) return null;

            List<String> metrics = new ArrayList<>(map.keySet());
            String name = metrics.get(0);
            return new Tuple2oo<>(name, map.get(name));
        }
        return null;
    }

    /**
     * Selects a grid search given the name of an algorithm.
     *
     * @param algorithm the name of the algorithm.
     *
     * @return if the algorithm exists, returns its grid search, null otherwise.
     */
    public RecommMetricConfigurator<U, F> selectGridSearch(String algorithm)
    {
        return switch (algorithm)
        {
            case P -> new PrecisionConfigurator<>();
            case R -> new RecallConfigurator<>();
            case NDCG -> new NDCGConfigurator<>();
            case MAP -> new MAPConfigurator<>();

            case ILD -> new IntraListDiversityConfigurator<>();
            case LTN -> new LongTailNoveltyConfigurator<>();
            case MPD -> new MeanPredictionDistanceConfigurator<>();
            case UNEXP -> new UnexpectednessConfigurator<>();
            case PGC -> new PredictionGiniComplementConfigurator<>();

            case CRECALL -> new CommunityRecallConfigurator<>();
            case ERRIA -> new ERRIAConfigurator<>();

            default -> null;
        };
    }
}
