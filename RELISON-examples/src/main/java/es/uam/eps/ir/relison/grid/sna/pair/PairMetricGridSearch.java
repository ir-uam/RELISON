/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.sna.pair;

import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.sna.metrics.PairMetric;
import es.uam.eps.ir.relison.sna.metrics.distance.DistanceCalculator;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Class for performing the grid search for a metric for a pair of nodes.
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @param <U> Type of the users
 */
public interface PairMetricGridSearch<U>
{
    /**
     * Obtains the different metrics for pairs of nodes to compute in a grid.
     * @param grid      the grid for the metric
     * @param distCalc  a distance calculator.
     * @return a map containing suppliers for the metrics, indexed by name.
     */
    Map<String, Supplier<PairMetric<U>>> grid(Grid grid, DistanceCalculator<U> distCalc);
    
    /**
     * Obtains the different metrics for pairs of nodes to compute in a grid.
     * @param grid the grid for the metric
     * @return a map containing suppliers for the metrics which depend on a distance calculator, indexed by name.
     */
    Map<String, PairMetricFunction<U>> grid(Grid grid);
}
