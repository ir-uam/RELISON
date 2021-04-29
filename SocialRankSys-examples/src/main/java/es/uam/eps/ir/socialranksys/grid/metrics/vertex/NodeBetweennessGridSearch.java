/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.metrics.vertex;

import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.metrics.VertexMetric;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.socialranksys.metrics.distance.vertex.NodeBetweenness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.metrics.vertex.VertexMetricIdentifiers.BETWEENNESS;

/**
 * Grid for the betweenness of a node.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 *
 * @see es.uam.eps.ir.socialranksys.metrics.distance.vertex.NodeBetweenness
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
        normalize.forEach(norm -> metrics.put(BETWEENNESS + "_" + (norm ? "norm" : "notnorm"), () -> new NodeBetweenness<>(distCalc, norm)));
        return metrics;
    }

    @Override
    public Map<String, VertexMetricFunction<U>> grid(Grid grid)
    {
        Map<String, VertexMetricFunction<U>> metrics = new HashMap<>();
        List<Boolean> normalize = grid.getBooleanValues(NORM);
        normalize.forEach(norm -> metrics.put(BETWEENNESS + "_" + (norm ? "norm" : "notnorm"), (distCalc) -> new NodeBetweenness<>(distCalc, norm)));
        return metrics;
    }
    
}
