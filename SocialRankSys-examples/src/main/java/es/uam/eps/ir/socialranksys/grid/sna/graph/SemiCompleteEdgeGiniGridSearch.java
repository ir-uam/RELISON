/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.sna.graph;

import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.metrics.GraphMetric;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.socialranksys.metrics.graph.SemiCompleteEdgeGini;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.sna.graph.GraphMetricIdentifiers.SEMICOMPLETEEDGEGINI;

/**
 * Grid for the edge Gini between all pairs of nodes in a graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class SemiCompleteEdgeGiniGridSearch<U> implements GraphMetricGridSearch<U> 
{    
    @Override
    public Map<String, Supplier<GraphMetric<U>>> grid(Grid grid, DistanceCalculator<U> distCalc)
    {
        Map<String, Supplier<GraphMetric<U>>> metrics = new HashMap<>();
        
        metrics.put(SEMICOMPLETEEDGEGINI, SemiCompleteEdgeGini::new);
        return metrics;
    }

    @Override
    public Map<String, GraphMetricFunction<U>> grid(Grid grid)
    {
        Map<String, GraphMetricFunction<U>> metrics = new HashMap<>();
        
        metrics.put(SEMICOMPLETEEDGEGINI, (distCalc) -> new SemiCompleteEdgeGini<>());
        return metrics;
    }
    
}
