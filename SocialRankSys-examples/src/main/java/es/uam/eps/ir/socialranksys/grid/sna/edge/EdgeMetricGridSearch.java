/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.sna.edge;

import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.metrics.EdgeMetric;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Class for performing the grid search for a given algorithm.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public interface EdgeMetricGridSearch<U>
{
    /**
     * Obtains the different edge metrics to compute in a grid.
     * @param grid The grid for the metric
     * @param distCalc The distance calculator
     * @return a map containing suppliers for obtaining the different variations of the metric.
     */
    Map<String, Supplier<EdgeMetric<U>>> grid(Grid grid, DistanceCalculator<U> distCalc);
    
    /**
     * Obtains functions for obtaining the different edge metrics to compute in a grid given a distance calculator.
     * @param grid The grid for the metric.
     * @return a map containing functions for obtaining the different variations of the metric given a distance calculator.
     */
    Map<String, EdgeMetricFunction<U>> grid(Grid grid);
}
