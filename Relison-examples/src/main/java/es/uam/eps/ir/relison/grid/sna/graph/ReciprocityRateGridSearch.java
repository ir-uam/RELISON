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
import es.uam.eps.ir.relison.sna.metrics.GraphMetric;
import es.uam.eps.ir.relison.sna.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.relison.sna.metrics.graph.ReciprocityRate;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.relison.grid.sna.graph.GraphMetricIdentifiers.RECIPROCITYRATE;

/**
 * Grid for the reciprocity rate of the graph.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 */
public class ReciprocityRateGridSearch<U> implements GraphMetricGridSearch<U> 
{    
    @Override
    public Map<String, Supplier<GraphMetric<U>>> grid(Grid grid, DistanceCalculator<U> distCalc)
    {
        Map<String, Supplier<GraphMetric<U>>> metrics = new HashMap<>();
        metrics.put(RECIPROCITYRATE, ReciprocityRate::new);
        return metrics;
    }

    @Override
    public Map<String, GraphMetricFunction<U>> grid(Grid grid)
    {
        Map<String, GraphMetricFunction<U>> metrics = new HashMap<>();
        metrics.put(RECIPROCITYRATE, (distCalc) -> new ReciprocityRate<>());
        return metrics;
    }
    
}
