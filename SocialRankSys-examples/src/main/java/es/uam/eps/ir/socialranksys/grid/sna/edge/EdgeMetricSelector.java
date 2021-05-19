/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.uam.eps.ir.socialranksys.grid.sna.edge;

import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.sna.graph.GraphMetricFunction;
import es.uam.eps.ir.socialranksys.metrics.EdgeMetric;
import es.uam.eps.ir.socialranksys.metrics.GraphMetric;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.socialranksys.metrics.graph.aggregate.AggregateEdgeMetric;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.sna.edge.EdgeMetricIdentifiers.*;


/**
 * Class that translates from a grid to the different contact recommendation algorithns.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class EdgeMetricSelector<U>
{
    /**
     * Obtains the grid configurator for a metric.
     * @param metric the name of the metric.
     * @return the grid configurator for the metric if it exists, null otherwise.
     */
    public EdgeMetricGridSearch<U> getGridSearch(String metric)
    {
        EdgeMetricGridSearch<U> gridsearch;
        switch(metric)
        {
            case BETWEENNESS:
                gridsearch = new EdgeBetweennessGridSearch<>();
                break;
            case EMBEDEDNESS:
                gridsearch = new EmbedednessGridSearch<>();
                break;
            case WEIGHT:
                gridsearch = new EdgeWeightGridSearch<>();
                break;
            case FOAF:
                gridsearch = new FOAFGridSearch<>();
                break;
            case COMPLEMBEDEDNESS:
                gridsearch = new ComplementaryEmbedednessGridSearch<>();
                break;
            case COMPLFOAF:
                gridsearch = new ComplementaryFOAFGridSearch<>();
                break;
            // Default behavior
            default:
                gridsearch = null;
        }
        
        return gridsearch;
    }
    
    /**
     * Obtains the different variants of the available algorithms using the different
     * parameters in the grid.
     * @param metric the name of the metric.
     * @param grid the parameter grid.
     * @param distCalc a distance calculator.
     * @return a map containing the different metric suppliers.
     */
    public Map<String, Supplier<EdgeMetric<U>>> getMetrics(String metric, Grid grid, DistanceCalculator<U> distCalc)
    {
        EdgeMetricGridSearch<U> gridsearch = this.getGridSearch(metric);
                
        if(gridsearch != null)
            return gridsearch.grid(grid, distCalc);
        return new HashMap<>();
    }
    
    /**
     * Obtains the different variants of a given edge metric depending on the 
     * parameters selected in a grid.
     * @param metric the name of the metric.
     * @param grid the grid containing the different parameters.
     * @return a map containing the different metric suppliers.
     */
    public Map<String, EdgeMetricFunction<U>> getMetrics(String metric, Grid grid)
    {
        EdgeMetricGridSearch<U> gridsearch = this.getGridSearch(metric);
                
        if(gridsearch != null)
            return gridsearch.grid(grid);
        return new HashMap<>();
    }
    
    /**
     * Obtains the aggregate variants of a given edge metric, given the parameters selected
     * in a grid.
     * @param metric the name of the metric.
     * @param grid the grid containing the different parameters.
     * @param distCalc a distance calculator.
     * @return a map, indexed by metric name, containing the different variants of the metric selected in the grid.
     */
    public Map<String, Supplier<GraphMetric<U>>> getGraphMetrics(String metric, Grid grid, DistanceCalculator<U> distCalc)
    {
        EdgeMetricGridSearch<U> gridsearch = this.getGridSearch(metric);
        if(gridsearch != null)
        {
            Map<String, Supplier<EdgeMetric<U>>> metrics = gridsearch.grid(grid, distCalc);
            Map<String, Supplier<GraphMetric<U>>> graphMetrics = new HashMap<>();
            metrics.forEach((name, value) -> graphMetrics.put(name, () -> new AggregateEdgeMetric<>(value.get())));
            return graphMetrics;
        }
        return new HashMap<>();
    }
    
    /**
     * Obtains the aggregate variants of a given edge metric, given the parameters selected
     * in a grid.
     * @param metric the name of the metric.
     * @param grid the grid containing the different parameters.
     * @return a map, indexed by metric name, containing the different variants of the metric selected in the grid.
     */
    public Map<String, GraphMetricFunction<U>> getGraphMetrics(String metric, Grid grid)
    {
        EdgeMetricGridSearch<U> gridsearch = this.getGridSearch(metric);
        if(gridsearch != null)
        {
            Map<String, EdgeMetricFunction<U>> metrics = gridsearch.grid(grid);
            Map<String, GraphMetricFunction<U>> graphMetrics = new HashMap<>();
            metrics.forEach((name, value) -> graphMetrics.put(name, (distCalc) -> new AggregateEdgeMetric<>(value.apply(distCalc))));
            return graphMetrics;
        }
        return new HashMap<>();
    }
}
