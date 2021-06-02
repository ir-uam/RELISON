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
import es.uam.eps.ir.sonalire.metrics.complementary.vertex.ComplementaryPageRank;
import es.uam.eps.ir.sonalire.metrics.distance.DistanceCalculator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.sonalire.grid.sna.vertex.VertexMetricIdentifiers.COMPLPAGERANK;


/**
 * Grid for the PageRank value of a node in the complementary graph.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 *
 * @see ComplementaryPageRank
 */
public class ComplementaryPageRankGridSearch<U> implements VertexMetricGridSearch<U> 
{
    /**
     * Identifier for the orientation.
     */
    private static final String R = "r";

    @Override
    public Map<String, Supplier<VertexMetric<U>>> grid(Grid grid, DistanceCalculator<U> distCalc)
    {
        Map<String, Supplier<VertexMetric<U>>> metrics = new HashMap<>();
        List<Double> rs = grid.getDoubleValues(R);
        
        rs.forEach(r ->
            metrics.put(COMPLPAGERANK + "_" + r, () -> new ComplementaryPageRank<>(r)));
        return metrics;
    }

    @Override
    public Map<String, VertexMetricFunction<U>> grid(Grid grid)
    {
        Map<String, VertexMetricFunction<U>> metrics = new HashMap<>();
        List<Double> rs = grid.getDoubleValues(R);
        
        rs.forEach(r ->
            metrics.put(COMPLPAGERANK + "_" + r, (distCalc) -> new ComplementaryPageRank<>(r)));
        
        return metrics;
    }
    
}
