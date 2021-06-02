/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.sna.pair;

import es.uam.eps.ir.sonalire.grid.Configurations;
import es.uam.eps.ir.sonalire.grid.Grid;
import es.uam.eps.ir.sonalire.grid.Parameters;
import es.uam.eps.ir.sonalire.grid.sna.graph.GraphMetricFunction;
import es.uam.eps.ir.sonalire.metrics.GraphMetric;
import es.uam.eps.ir.sonalire.metrics.PairMetric;
import es.uam.eps.ir.sonalire.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.sonalire.metrics.graph.aggregate.AggregatePairMetric;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;


/**
 * Class that translates from a grid to the different pair metrics.
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @param <U> type of the users.
 */
public class PairMetricSelector<U>
{
     /**
     * Obtains the grid configurator for a metric.
     * @param metric the name of the metric.
     * @return the grid configurator for the metric if it exists, null otherwise.
     */
    public PairMetricGridSearch<U> getGridSearch(String metric)
    {
        // Default behavior
        return switch (metric)
        {
            case PairMetricIdentifiers.BETWEENNESS -> new EdgeBetweennessGridSearch<>();
            case PairMetricIdentifiers.DISTANCE -> new DistanceGridSearch<>();
            case PairMetricIdentifiers.GEODESICS -> new GeodesicsGridSearch<>();
            case PairMetricIdentifiers.DISTANCEWITHOUTLINK -> new DistanceWithoutLinkGridSearch<>();
            case PairMetricIdentifiers.EMBEDEDNESS -> new EmbedednessGridSearch<>();
            case PairMetricIdentifiers.COMPLEMBEDEDNESS -> new ComplementaryEmbedednessGridSearch<>();
            case PairMetricIdentifiers.COMPLFOAF -> new ComplementaryFOAFGridSearch<>();
            case PairMetricIdentifiers.RECIP -> new ReciprocityRateGridSearch<>();
            case PairMetricIdentifiers.RECIPROCALSPL -> new ReciprocalShortestPathLengthGridSearch<>();
            case PairMetricIdentifiers.SHRINKINGDIAM -> new ShrinkingDiameterGridSearch<>();
            case PairMetricIdentifiers.SHRINKINGASL -> new ShrinkingASLGridSearch<>();
            case PairMetricIdentifiers.SHRINKINGASLNEIGH -> new ShrinkingASLNeighborsGridSearch<>();
            case PairMetricIdentifiers.SHRINKINGDIAMNEIGH -> new ShrinkingDiameterNeighborsGridSearch<>();
            case PairMetricIdentifiers.CCINCREASE -> new ClusteringCoefficientIncrementGridSearch<>();
            case PairMetricIdentifiers.FOAF -> new FOAFGridSearch<>();
            case PairMetricIdentifiers.WFOAF -> new WeightedFOAFGridSearch<>();
            case PairMetricIdentifiers.WFOAFLOG -> new WeightedFOAFLogGridSearch<>();
            case PairMetricIdentifiers.EFOAF -> new ExpandedFOAFGridSearch<>();
            case PairMetricIdentifiers.EFOAFCOUNT -> new ExpandedFOAFCountGridSearch<>();
            case PairMetricIdentifiers.PREFATTACH -> new PreferentialAttachmentGridSearch<>();
            default -> null;
        };
    }
    
    /**
     * Obtains the different variants of a given pair metric depending on the 
     * parameters selected in a grid.
     * @param name      the name of the metric.
     * @param grid      the grid containing the different parameters.
     * @param distcalc  a distance calculator.
     * @return a map containing the different metric suppliers.
     */
    public Map<String, Supplier<PairMetric<U>>> getMetrics(String name, Grid grid, DistanceCalculator<U> distcalc)
    {
        PairMetricGridSearch<U> gridsearch = this.getGridSearch(name);
        
        if(gridsearch != null)
            return gridsearch.grid(grid, distcalc);
        return new HashMap<>();
    }
    
    /**
     * Obtains the different variants of a given pair metric depending on the 
     * parameters selected in a grid.
     * @param name the name of the metric.
     * @param grid the grid containing the different parameters.
     * @return a map containing the different metric suppliers, which work given a distance calculator
     */
    public Map<String, PairMetricFunction<U>> getMetrics(String name, Grid grid)
    {
        PairMetricGridSearch<U> gridsearch = this.getGridSearch(name);
        
        if(gridsearch != null)
            return gridsearch.grid(grid);
        return new HashMap<>();
    }
    
    /**
     * Obtains the aggregate variants of a given pair metric, given the parameters selected
     * in a grid.
     * @param metric    the name of the metric.
     * @param grid      the grid containing the different parameters.
     * @param distCalc  a distance calculator.
     * @return a map, indexed by metric name, containing the different variants of the metric selected in the grid.
     */
    public Map<String, Supplier<GraphMetric<U>>> getGraphMetrics(String metric, Grid grid, DistanceCalculator<U> distCalc)
    {
        PairMetricGridSearch<U> gridsearch = this.getGridSearch(metric);
        if(gridsearch != null)
        {
            Map<String, Supplier<PairMetric<U>>> metrics = gridsearch.grid(grid, distCalc);
            Map<String, Supplier<GraphMetric<U>>> graphMetrics = new HashMap<>();
            metrics.forEach((name, value) -> graphMetrics.put(name, () -> new AggregatePairMetric<>(value.get())));
            return graphMetrics;
        }
        return new HashMap<>();
    }
    
    /**
     * Obtains the aggregate variants of a given pair metric, given the parameters selected
     * in a grid.
     * @param metric    the name of the metric.
     * @param grid      the grid containing the different parameters.
     * @return a map, indexed by metric name, containing the different variants of the metric selected in the grid.
     */
    public Map<String, GraphMetricFunction<U>> getGraphMetrics(String metric, Grid grid)
    {
        PairMetricGridSearch<U> gridsearch = this.getGridSearch(metric);
        if(gridsearch != null)
        {
            Map<String, PairMetricFunction<U>> metrics = gridsearch.grid(grid);
            Map<String, GraphMetricFunction<U>> graphMetrics = new HashMap<>();
            metrics.forEach((name, value) -> graphMetrics.put(name, (distCalc) -> new AggregatePairMetric<>(value.apply(distCalc))));
            return graphMetrics;
        }
        return new HashMap<>();
    }

    /**
     * Obtains the different variants of a given pair metric depending on the
     * parameters selected in a grid.
     * @param metric    the name of the metric.
     * @param confs     the configurations.
     * @return a map containing the different metric suppliers, which work given a distance calculator.
     */
    public Map<String, PairMetricFunction<U>> getMetrics(String metric, Configurations confs)
    {
        Map<String, PairMetricFunction<U>> metrics = new HashMap<>();
        PairMetricGridSearch<U> gridsearch = this.getGridSearch(metric);
        if (gridsearch != null)
        {
            for (Parameters params : confs.getConfigurations())
            {
                Grid grid = params.toGrid();
                Map<String, PairMetricFunction<U>> map = getMetrics(metric, grid);
                if (map == null || map.isEmpty()) return null;

                List<String> algs = new ArrayList<>(map.keySet());
                String name = algs.get(0);

                metrics.put(name, map.get(name));
            }

            return metrics;
        }
        return new HashMap<>();
    }
}
