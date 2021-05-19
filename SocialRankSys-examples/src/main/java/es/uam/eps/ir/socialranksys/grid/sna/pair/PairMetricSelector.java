/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.uam.eps.ir.socialranksys.grid.sna.pair;

import es.uam.eps.ir.socialranksys.grid.Configurations;
import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.Parameters;
import es.uam.eps.ir.socialranksys.grid.sna.graph.GraphMetricFunction;
import es.uam.eps.ir.socialranksys.metrics.GraphMetric;
import es.uam.eps.ir.socialranksys.metrics.PairMetric;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.socialranksys.metrics.graph.aggregate.AggregatePairMetric;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.sna.pair.PairMetricIdentifiers.*;


/**
 * Class that translates from a grid to the different contact recommendation algorithns.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
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
        PairMetricGridSearch<U> gridsearch;
        switch(metric)
        {
            case DISTANCE:
                gridsearch = new DistanceGridSearch<>();
                break;
            case GEODESICS:
                gridsearch = new GeodesicsGridSearch<>();
                break;
            case DISTANCEWITHOUTLINK:
                gridsearch = new DistanceWithoutLinkGridSearch<>();
                break;
            case EMBEDEDNESS:
                gridsearch = new EmbedednessGridSearch<>();
                break;
            case COMPLEMBEDEDNESS:
                gridsearch = new ComplementaryEmbedednessGridSearch<>();
                break;
            case COMPLFOAF:
                gridsearch = new ComplementaryFOAFGridSearch<>();
                break;
            case RECIP:
                gridsearch = new ReciprocityRateGridSearch<>();
                break;
            case RECIPROCALSPL:
                gridsearch = new ReciprocalShortestPathLengthGridSearch<>();
                break;
            case SHRINKINGDIAM:
                gridsearch = new ShrinkingDiameterGridSearch<>();
                break;
            case SHRINKINGASL:
                gridsearch = new ShrinkingASLGridSearch<>();
                break;
            case SHRINKINGASLNEIGH:
                gridsearch = new ShrinkingASLNeighborsGridSearch<>();
                break;
            case SHRINKINGDIAMNEIGH:
                gridsearch = new ShrinkingDiameterNeighborsGridSearch<>();
                break;
            case CCINCREASE:
                gridsearch = new ClusteringCoefficientIncrementGridSearch<>();
                break;
            case FOAF:
                gridsearch = new FOAFGridSearch<>();
                break;
            case WFOAF:
                gridsearch = new WeightedFOAFGridSearch<>();
                break;
            case WFOAFLOG:
                gridsearch = new WeightedFOAFLogGridSearch<>();
                break;
            case EFOAF:
                gridsearch = new ExpandedFOAFGridSearch<>();
                break;
            case EFOAFCOUNT:
                gridsearch = new ExpandedFOAFCountGridSearch<>();
                break;
            case PREFATTACH:
                gridsearch = new PreferentialAttachmentGridSearch<>();
                break;
            // Default behavior
            default:
                gridsearch = null;
        }
        
        return gridsearch;
    }
    
        /**
     * Obtains the different variants of a given pair metric depending on the 
     * parameters selected in a grid.
     * @param name the name of the metric.
     * @param grid the grid containing the different parameters.
     * @param distcalc a distance calculator.
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
     * @param metric the name of the metric.
     * @param grid the grid containing the different parameters.
     * @param distCalc a distance calculator.
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
     * @param metric the name of the metric.
     * @param grid the grid containing the different parameters.
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
