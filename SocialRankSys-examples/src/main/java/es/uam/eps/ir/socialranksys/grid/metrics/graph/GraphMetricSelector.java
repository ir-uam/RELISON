/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.uam.eps.ir.socialranksys.grid.metrics.graph;

import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.metrics.GraphMetric;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.metrics.graph.GraphMetricIdentifiers.*;

/**
 * Class that translates from a grid to the different contact recommendation algorithns.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class GraphMetricSelector<U>
{
    /**
     * Obtains the grid configurator for a metric.
     * @param metric the name of the metric.
     * @return the grid configurator for the metric if it exists, null otherwise.
     */
    public GraphMetricGridSearch<U> getGridSearch(String metric)
    {
        GraphMetricGridSearch<U> gridsearch;
        switch(metric)
        {
            case ASL:
                gridsearch = new ASLGridSearch<>();
                break;
            case ARSL:
                gridsearch = new AverageReciprocalShortestPathLengthGridSearch<>();
                break;
            case DIAMETER:
                gridsearch = new DiameterGridSearch<>();
                break;
            case CLUSTCOEF:
                gridsearch = new ClusteringCoefficientGridSearch<>();
                break;
            case CLUSTCOEFCOMPL:
                gridsearch = new ClusteringCoefficientComplementGridSearch<>();
                break;
            case DEGREEGINI:
                gridsearch = new DegreeGiniGridSearch<>();
                break;
            case DENSITY:
                gridsearch = new DensityGridSearch<>();
                break;
            case NUMEDGES:
                gridsearch = new NumEdgesGridSearch<>();
                break;
            case INTEREDGEGINI:
                gridsearch = new InterEdgeGiniGridSearch<>();
                break;
            case COMPLETEEDGEGINI:
                gridsearch = new CompleteEdgeGiniGridSearch<>();
                break;
            case SEMICOMPLETEEDGEGINI:
                gridsearch = new SemiCompleteEdgeGiniGridSearch<>();
                break;
            case RECIPRDIAMETER:
                gridsearch = new ReciprocalDiameterGridSearch<>();
                break;
            case RECIPRAVGECCENTRICITY:
                gridsearch = new ReciprocalAverageEccentricityGridSearch<>();
                break;
            case RECIPROCITYRATE:
                gridsearch = new ReciprocityRateGridSearch<>();
                break;
            case DEGREEASSORT:
                gridsearch = new DegreeAssortativityGridSearch<>();
                break;
            case DEGREEPEARSON:
                gridsearch = new DegreePearsonCorrelationGridSearch<>();
                break;
            // Default behavior
            default:
                gridsearch = null;
        }
        
        return gridsearch;
    }
    
    /**
     * Obtains the different variants of a given global graph metric depending on the 
     * parameters selected in a grid.
     * @param name the name of the metric.
     * @param grid the grid containing the different parameters.
     * @param distcalc a distance calculator.
     * @return a map containing the different metric suppliers.
     */
    public Map<String, Supplier<GraphMetric<U>>> getMetrics(String name, Grid grid, DistanceCalculator<U> distcalc)
    {
        GraphMetricGridSearch<U> gridsearch = this.getGridSearch(name);
        
        if(gridsearch != null)
            return gridsearch.grid(grid, distcalc);
        return new HashMap<>();
    }
    
    /**
     * Obtains the different variants of a given global graph metric depending on the 
     * parameters selected in a grid.
     * @param name the name of the metric.
     * @param grid the grid containing the different parameters.
     * @return a map containing the different metric suppliers, which work given a distance calculator
     */
    public Map<String, GraphMetricFunction<U>> getMetrics(String name, Grid grid)
    {
        GraphMetricGridSearch<U> gridsearch = this.getGridSearch(name);
        
        if(gridsearch != null)
            return gridsearch.grid(grid);
        return new HashMap<>();
    }
}
