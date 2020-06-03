/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.uam.eps.ir.socialranksys.grid.metrics.vertex;


import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.metrics.graph.GraphMetricFunction;
import es.uam.eps.ir.socialranksys.metrics.GraphMetric;
import es.uam.eps.ir.socialranksys.metrics.VertexMetric;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.socialranksys.metrics.graph.aggregate.AggregateVertexMetric;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.metrics.vertex.VertexMetricIdentifiers.*;


/**
 * Class that translates from a grid to the different contact recommendation algorithns.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
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
        VertexMetricGridSearch<U> gridsearch;
        switch(metric)
        {
            case CLOSENESS:
                gridsearch = new ClosenessGridSearch<>();
                break;
            case BETWEENNESS:
                gridsearch = new NodeBetweennessGridSearch<>();
                break;
            case ECCENTRICITY:
                gridsearch = new EccentricityGridSearch<>();
                break;
            case DEGREE:
                gridsearch = new DegreeGridSearch<>();
                break;
            case HITS:
                gridsearch = new HITSGridSearch<>();
                break;
            case INVDEGREE:
                gridsearch = new InverseDegreeGridSearch<>();
                break;
            case LOCALCLUSTCOEF:
                gridsearch = new LocalClusteringCoefficientGridSearch<>();
                break;
            case LOCALRECIPRATE:
                gridsearch = new LocalReciprocityRateGridSearch<>();
                break;
            case PAGERANK:
                gridsearch = new PageRankGridSearch<>();
                break;
            case COMPLDEGREE:
                gridsearch = new ComplementaryDegreeGridSearch<>();
                break;
            case COMPLINVDEGREE:
                gridsearch = new ComplementaryInverseDegreeGridSearch<>();
                break;
            case COMPLLOCALCLUSTCOEF:
                gridsearch = new ComplementaryLocalClusteringCoefficientGridSearch<>();
                break;
            case COMPLPAGERANK:
                gridsearch = new ComplementaryPageRankGridSearch<>();
                break;
            case FD:
                gridsearch = new FreeDiscoveryGridSearch<>();
                break;
            case LENGTH:
                gridsearch = new VertexLengthGridSearch<>();
                break;
            // Default behavior
            default:
                gridsearch = null;
        }
        
        return gridsearch;
    }
        
    /**
     * Obtains the different variants of a given vertex metric depending on the 
     * parameters selected in a grid.
     * @param metric the name of the metric.
     * @param grid the grid containing the different parameters.
     * @param distCalc a distance calculator.
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
     * @param metric the name of the metric.
     * @param grid the grid containing the different parameters.
     * @return a map containing the different metric suppliers, which work given a distance calculator
     */
    public Map<String, VertexMetricFunction<U>> getMetrics(String metric, Grid grid)
    {
        VertexMetricGridSearch<U> gridsearch = this.getGridSearch(metric);
        if(gridsearch != null)
            return gridsearch.grid(grid);
        return new HashMap<>();
    }
    
    /**
     * Obtains the aggregate variants of a given vertex metric, given the parameters selected
     * in a grid.
     * @param metric the name of the metric.
     * @param grid the grid containing the different parameters.
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
     * @param metric the name of the metric.
     * @param grid the grid containing the different parameters.
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
