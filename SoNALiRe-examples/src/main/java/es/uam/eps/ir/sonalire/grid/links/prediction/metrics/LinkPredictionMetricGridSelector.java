/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es.
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
import es.uam.eps.ir.sonalire.grid.Configurations;
import es.uam.eps.ir.sonalire.grid.Grid;
import es.uam.eps.ir.sonalire.grid.Parameters;
import es.uam.eps.ir.sonalire.grid.links.prediction.metrics.accuracy.*;
import es.uam.eps.ir.sonalire.links.linkprediction.metrics.LinkPredictionMetric;
import es.uam.eps.ir.sonalire.utils.datatypes.Tuple2oo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.sonalire.grid.links.prediction.metrics.LinkPredictionMetricIdentifiers.*;

/**
 * Class that translates from a grid to different link prediction metrics.
 *
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class LinkPredictionMetricGridSelector<U, F>
{
    /**
     * Constructor.
     */
    public LinkPredictionMetricGridSelector()
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
    public Map<String, Supplier<LinkPredictionMetric<U>>> getMetrics(String metric, Grid grid, Graph<U> trainGraph, Graph<U> testGraph, PreferenceData<U,U> trainData, PreferenceData<U,U> testData, FeatureData<U,F,Double> featData, Communities<U> comms)
    {
        LinkPredictionMetricConfigurator<U,F> gridsearch = this.selectGridSearch(metric);
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
    public Tuple2oo<String, Supplier<LinkPredictionMetric<U>>> getMetrics(String metric, Parameters params, Graph<U> trainGraph, Graph<U> testGraph, PreferenceData<U,U> trainData, PreferenceData<U,U> testData, FeatureData<U,F,Double> featData, Communities<U> comms)
    {
        LinkPredictionMetricConfigurator<U, F> gridsearch = this.selectGridSearch(metric);
        if (gridsearch != null)
        {
            Grid grid = params.toGrid();
            Map<String, Supplier<LinkPredictionMetric<U>>> map = this.getMetrics(metric, grid, trainGraph, testGraph, trainData, testData, featData, comms);
            if (map == null || map.isEmpty()) return null;

            List<String> metrics = new ArrayList<>(map.keySet());
            String name = metrics.get(0);
            Supplier<LinkPredictionMetric<U>> supplier = map.get(name);
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
    public Map<String, Supplier<LinkPredictionMetric<U>>> getMetrics(String metric, Configurations configs, Graph<U> trainGraph, Graph<U> testGraph, PreferenceData<U,U> trainData, PreferenceData<U,U> testData, FeatureData<U,F,Double> featData, Communities<U> comms)
    {
        Map<String, Supplier<LinkPredictionMetric<U>>> metrics = new HashMap<>();
        LinkPredictionMetricConfigurator<U, F> gridSearch = this.selectGridSearch(metric);
        if (gridSearch != null)
        {
            for (Parameters params : configs.getConfigurations())
            {
                Grid grid = params.toGrid();
                Map<String, Supplier<LinkPredictionMetric<U>>> map = getMetrics(metric, grid, trainGraph, testGraph, trainData, testData, featData, comms);
                if (map == null || map.isEmpty()) return null;

                List<String> metr = new ArrayList<>(map.keySet());
                String name = metr.get(0);
                Supplier<LinkPredictionMetric<U>> supplier = map.get(name);
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
    public Map<String, LinkPredictionMetricFunction<U, F>> getMetrics(String metric, Grid grid)
    {
        LinkPredictionMetricConfigurator<U, F> gridsearch = this.selectGridSearch(metric);
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
    public Map<String, LinkPredictionMetricFunction<U, F>> getMetrics(String metric, Configurations configs)
    {
        LinkPredictionMetricConfigurator<U, F> gridsearch = this.selectGridSearch(metric);
        Map<String, LinkPredictionMetricFunction<U,F>> metrics = new HashMap<>();
        if (gridsearch != null)
        {
            for (Parameters params : configs.getConfigurations())
            {
                Grid grid = params.toGrid();
                Map<String, LinkPredictionMetricFunction<U, F>> map = getMetrics(metric, grid);
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
    public Tuple2oo<String, LinkPredictionMetricFunction<U, F>> getMetrics(String metric, Parameters params)
    {
        LinkPredictionMetricConfigurator<U, F> gridsearch = this.selectGridSearch(metric);
        if (gridsearch != null)
        {
            Grid grid = params.toGrid();
            Map<String, LinkPredictionMetricFunction<U, F>> map = getMetrics(metric, grid);
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
    public LinkPredictionMetricConfigurator<U, F> selectGridSearch(String algorithm)
    {
        return switch (algorithm)
        {
            case P -> new PrecisionConfigurator<>();
            case R -> new RecallConfigurator<>();
            case AUC -> new AUCConfigurator<>();
            case F1SCORE -> new F1ScoreConfigurator<>();
            case ACCURACY -> new AccuracyConfigurator<>();

            default -> null;
        };
    }
}
