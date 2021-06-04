/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.sna.vertex;

import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.metrics.VertexMetric;
import es.uam.eps.ir.relison.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.relison.metrics.vertex.FreeDiscovery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;


/**
 *
 * @author Javier
 */
public class FreeDiscoveryGridSearch<U> implements VertexMetricGridSearch<U> {
    
    private final static String ORIENT = "orient";
    @Override
    public Map<String, Supplier<VertexMetric<U>>> grid(Grid grid, DistanceCalculator<U> distCalc)
    {
        List<EdgeOrientation> orient = grid.getOrientationValues(ORIENT);
        
        Map<String, Supplier<VertexMetric<U>>> map = new HashMap<>();
        orient.forEach(or -> map.put(VertexMetricIdentifiers.FD + "_" + or, () -> new FreeDiscovery<>(or)));
        return map;
    }

    @Override
    public Map<String, VertexMetricFunction<U>> grid(Grid grid)
    {
        List<EdgeOrientation> orient = grid.getOrientationValues(ORIENT);
        
        Map<String, VertexMetricFunction<U>> map = new HashMap<>();
        orient.forEach(or -> map.put(VertexMetricIdentifiers.FD + "_" + or, (distCalc) -> new FreeDiscovery<>(or)));
        return map;
    }
}
