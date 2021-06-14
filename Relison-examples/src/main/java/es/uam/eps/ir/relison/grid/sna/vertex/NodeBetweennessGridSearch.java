/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.sna.vertex;

import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.sna.metrics.VertexMetric;
import es.uam.eps.ir.relison.sna.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.relison.sna.metrics.distance.vertex.NodeBetweenness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Grid for the betweenness of a node.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 *
 * @see NodeBetweenness
 */
public class NodeBetweennessGridSearch<U> implements VertexMetricGridSearch<U> 
{
    /**
     * Identifier of the value indicating whether to normalize the scores of the nodes or not.
     */
    private final static String NORM = "norm";


    @Override
    public Map<String, Supplier<VertexMetric<U>>> grid(Grid grid, DistanceCalculator<U> distCalc)
    {
        Map<String, Supplier<VertexMetric<U>>> metrics = new HashMap<>();
        List<Boolean> normalize = grid.getBooleanValues(NORM);
        normalize.forEach(norm -> metrics.put(VertexMetricIdentifiers.BETWEENNESS + "_" + (norm ? "norm" : "notnorm"), () -> new NodeBetweenness<>(distCalc, norm)));
        return metrics;
    }

    @Override
    public Map<String, VertexMetricFunction<U>> grid(Grid grid)
    {
        Map<String, VertexMetricFunction<U>> metrics = new HashMap<>();
        List<Boolean> normalize = grid.getBooleanValues(NORM);
        normalize.forEach(norm -> metrics.put(VertexMetricIdentifiers.BETWEENNESS + "_" + (norm ? "norm" : "notnorm"), (distCalc) -> new NodeBetweenness<>(distCalc, norm)));
        return metrics;
    }
    
}
