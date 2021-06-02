/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.sna.vertex;

import es.uam.eps.ir.sonalire.grid.Configurations;
import es.uam.eps.ir.sonalire.grid.Grid;
import es.uam.eps.ir.sonalire.grid.Parameters;
import es.uam.eps.ir.sonalire.grid.sna.graph.GraphMetricFunction;
import es.uam.eps.ir.sonalire.metrics.GraphMetric;
import es.uam.eps.ir.sonalire.metrics.VertexMetric;
import es.uam.eps.ir.sonalire.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.sonalire.metrics.graph.aggregate.AggregateVertexMetric;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.sonalire.grid.sna.vertex.VertexMetricIdentifiers.*;

/**
 * Class that translates from a grid to the different vertex metrics.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 */
public class VertexMetricSelector<U>
{   
    /**
     * Obtains the grid configurator for a metric.
     * @param metric the name of the metric.
     * @return the grid configurator for the metric if it exists, null otherwise.
     */
    public VertexMetricGridSearch<U> getGridSearch(String metric)
    {
        return switch (metric)
        {
            case CLOSENESS -> new ClosenessGridSearch<>();
            case HARMONIC -> new HarmonicCentralityGridSearch<>();
            case BETWEENNESS -> new NodeBetweennessGridSearch<>();
            case ECCENTRICITY -> new EccentricityGridSearch<>();
            case DEGREE -> new DegreeGridSearch<>();
            case HITS -> new HITSGridSearch<>();
            case INVDEGREE -> new InverseDegreeGridSearch<>();
            case LOCALCLUSTCOEF -> new LocalClusteringCoefficientGridSearch<>();
            case LOCALRECIPRATE -> new LocalReciprocityRateGridSearch<>();
            case PAGERANK -> new PageRankGridSearch<>();
            case COMPLDEGREE -> new ComplementaryDegreeGridSearch<>();
            case COMPLINVDEGREE -> new ComplementaryInverseDegreeGridSearch<>();
            case COMPLLOCALCLUSTCOEF -> new ComplementaryLocalClusteringCoefficientGridSearch<>();
            case COMPLPAGERANK -> new ComplementaryPageRankGridSearch<>();
            case FD -> new FreeDiscoveryGridSearch<>();
            case LENGTH -> new VertexLengthGridSearch<>();
            case CORENESS -> new CorenessGridSearch<>();
            case KATZ -> new KatzCentralityGridSearch<>();
            case EIGEN -> new EigenvectorCentralityGridSearch<>();
            default -> null;
        };
    }
        
    /**
     * Obtains the different variants of a given vertex metric depending on the 
     * parameters selected in a grid.
     * @param metric    the name of the metric.
     * @param grid      the grid containing the different parameters.
     * @param distCalc  a distance calculator.
     * @return a map containing the different metric suppliers.
     */
    public Map<String, Supplier<VertexMetric<U>>> getMetrics(String metric, Grid grid, DistanceCalculator<U> distCalc)
    {
        VertexMetricGridSearch<U> gridsearch = this.getGridSearch(metric);
        if(gridsearch != null)
            return gridsearch.grid(grid, distCalc);
        return new HashMap<>();
    }
    
    /**
     * Obtains the different variants of a given vertex metric depending on the 
     * parameters selected in a grid.
     * @param metric    the name of the metric.
     * @param grid      the grid containing the different parameters.
     * @return a map containing the different metric suppliers, which work given a distance calculator.
     */
    public Map<String, VertexMetricFunction<U>> getMetrics(String metric, Grid grid)
    {
        VertexMetricGridSearch<U> gridsearch = this.getGridSearch(metric);
        if(gridsearch != null)
            return gridsearch.grid(grid);
        return new HashMap<>();
    }

    /**
     * Obtains the different variants of a given vertex metric depending on the
     * parameters selected in a grid.
     * @param metric    the name of the metric.
     * @param confs     the configurations.
     * @return a map containing the different metric suppliers, which work given a distance calculator.
     */
    public Map<String, VertexMetricFunction<U>> getMetrics(String metric, Configurations confs)
    {
        Map<String, VertexMetricFunction<U>> metrics = new HashMap<>();
        VertexMetricGridSearch<U> gridsearch = this.getGridSearch(metric);
        if (gridsearch != null)
        {
            for (Parameters params : confs.getConfigurations())
            {
                Grid grid = params.toGrid();
                Map<String, VertexMetricFunction<U>> map = getMetrics(metric, grid);
                if (map == null || map.isEmpty()) return null;

                List<String> algs = new ArrayList<>(map.keySet());
                String name = algs.get(0);

                metrics.put(name, map.get(name));
            }

            return metrics;
        }
        return new HashMap<>();
    }


    /**
     * Obtains the aggregate variants of a given vertex metric, given the parameters selected
     * in a grid.
     * @param metric    the name of the metric.
     * @param grid      the grid containing the different parameters.
     * @param distCalc a distance calculator.
     * @return a map, indexed by metric name, containing the different variants of the metric selected in the grid.
     */
    public Map<String, Supplier<GraphMetric<U>>> getGraphMetrics(String metric, Grid grid, DistanceCalculator<U> distCalc)
    {
        VertexMetricGridSearch<U> gridsearch = this.getGridSearch(metric);
        if(gridsearch != null)
        {
            Map<String, Supplier<VertexMetric<U>>> metrics = gridsearch.grid(grid, distCalc);
            Map<String, Supplier<GraphMetric<U>>> graphMetrics = new HashMap<>();
            metrics.forEach((name, value) -> graphMetrics.put(name, () -> new AggregateVertexMetric<>(value.get())));
            return graphMetrics;
        }
        return new HashMap<>();
    }
    
    /**
     * Obtains the aggregate variants of a given vertex metric, given the parameters selected
     * in a grid.
     * @param metric    the name of the metric.
     * @param grid      the grid containing the different parameters.
     * @return a map, indexed by metric name, containing the different variants of the metric selected in the grid.
     */
    public Map<String, GraphMetricFunction<U>> getGraphMetrics(String metric, Grid grid)
    {
        VertexMetricGridSearch<U> gridsearch = this.getGridSearch(metric);
        if(gridsearch != null)
        {
            Map<String, VertexMetricFunction<U>> metrics = gridsearch.grid(grid);
            Map<String, GraphMetricFunction<U>> graphMetrics = new HashMap<>();
            metrics.forEach((name, value) -> graphMetrics.put(name, (distCalc) -> new AggregateVertexMetric<>(value.apply(distCalc))));
            return graphMetrics;
        }
        return new HashMap<>();
    }
}
