/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.sna.vertex;

import es.uam.eps.ir.sonalire.grid.Grid;
import es.uam.eps.ir.sonalire.metrics.VertexMetric;
import es.uam.eps.ir.sonalire.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.sonalire.metrics.vertex.HITS;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.sonalire.grid.sna.vertex.VertexMetricIdentifiers.HITS;


/**
 * Grid for the HITS value of a node.
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 *
 * @see HITS
 */
public class HITSGridSearch<U> implements VertexMetricGridSearch<U> 
{
    /**
     * Identifier for the orientation
     */
    private static final String MODE = "mode";

    @Override
    public Map<String, Supplier<VertexMetric<U>>> grid(Grid grid, DistanceCalculator<U> distCalc)
    {
                Map<String, Supplier<VertexMetric<U>>> metrics = new HashMap<>();
        List<Boolean> modes = grid.getBooleanValues(MODE);
        
        modes.forEach(mode ->
            metrics.put(HITS + "_" + (mode ? "auth" : "hubs"), () -> new HITS<>(mode)));
        
        return metrics;
    }

    @Override
    public Map<String, VertexMetricFunction<U>> grid(Grid grid)
    {
        Map<String, VertexMetricFunction<U>> metrics = new HashMap<>();
        List<Boolean> modes = grid.getBooleanValues(MODE);
        
        modes.forEach(mode ->
            metrics.put(HITS + "_" + (mode ? "auth" : "hubs"), (distCalc) -> new HITS<>(mode)));
        
        return metrics;
    }
    
}
