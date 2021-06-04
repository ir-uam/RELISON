/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.sna.graph;

import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.metrics.GraphMetric;
import es.uam.eps.ir.relison.metrics.distance.DistanceCalculator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.relison.grid.sna.graph.GraphMetricIdentifiers.*;

/**
 * Class that translates from a grid to the different global metrics of the social network graph.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
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
        return switch (metric)
        {
            case ASL -> new ASLGridSearch<>();
            case ARSL -> new AverageReciprocalShortestPathLengthGridSearch<>();
            case DIAMETER -> new DiameterGridSearch<>();
            case RADIUS -> new RadiusGridSearch<>();
            case INFINITEDIST -> new InfiniteDistancePairsGridSearch<>();
            case CLUSTCOEF -> new ClusteringCoefficientGridSearch<>();
            case CLUSTCOEFCOMPL -> new ClusteringCoefficientComplementGridSearch<>();
            case DEGREEGINI -> new DegreeGiniGridSearch<>();
            case DENSITY -> new DensityGridSearch<>();
            case NUMEDGES -> new NumEdgesGridSearch<>();
            case INTEREDGEGINI -> new InterEdgeGiniGridSearch<>();
            case COMPLETEEDGEGINI -> new CompleteEdgeGiniGridSearch<>();
            case SEMICOMPLETEEDGEGINI -> new SemiCompleteEdgeGiniGridSearch<>();
            case RECIPRDIAMETER -> new ReciprocalDiameterGridSearch<>();
            case RECIPRAVGECCENTRICITY -> new ReciprocalAverageEccentricityGridSearch<>();
            case RECIPROCITYRATE -> new ReciprocityRateGridSearch<>();
            case DEGREEASSORT -> new DegreeAssortativityGridSearch<>();
            case DEGREEPEARSON -> new DegreePearsonCorrelationGridSearch<>();
            default -> null;
        };
    }
    
    /**
     * Obtains the different variants of a given global graph metric depending on the 
     * parameters selected in a grid.
     * @param name      the name of the metric.
     * @param grid      the grid containing the different parameters.
     * @param distcalc  a distance calculator.
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
