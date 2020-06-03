/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.metrics.graph;

import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.metrics.GraphMetric;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Class for performing the grid search for a given graph metric..
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public interface GraphMetricGridSearch<U>
{
    /**
     * Obtains the different vertex metrics to compute in a grid.
     * @param grid The grid for the metric.
     * @param distCalc a distance calculator.
     * @return the grid parameters.
     */
    Map<String, Supplier<GraphMetric<U>>> grid(Grid grid, DistanceCalculator<U> distCalc);
    
    /**
     * Obtains the different global graph metrics to compute in a grid.
     * @param grid The grid for the metric.
     * @return the grid parameters.
     */
    Map<String, GraphMetricFunction<U>> grid(Grid grid);
}
