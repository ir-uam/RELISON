/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.sna.graph;

import es.uam.eps.ir.sonalire.grid.Grid;
import es.uam.eps.ir.sonalire.metrics.GraphMetric;
import es.uam.eps.ir.sonalire.metrics.distance.DistanceCalculator;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Class for performing the parameter configuration of global structural network metrics.
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @param <U> type of the users
 */
public interface GraphMetricGridSearch<U>
{
    /**
     * Obtains the different vertex metrics to compute in a grid.
     * @param grid      the grid for the metric.
     * @param distCalc  a distance calculator.
     * @return the grid parameters.
     */
    Map<String, Supplier<GraphMetric<U>>> grid(Grid grid, DistanceCalculator<U> distCalc);
    
    /**
     * Obtains the different global graph metrics to compute in a grid.
     * @param grid the grid for the metric.
     * @return the grid parameters.
     */
    Map<String, GraphMetricFunction<U>> grid(Grid grid);
}
