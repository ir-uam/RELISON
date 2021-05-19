/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
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
import es.uam.eps.ir.socialranksys.metrics.distance.edge.EdgeBetweenness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.sna.edge.EdgeMetricIdentifiers.BETWEENNESS;


/**
 * Grid search for the edge betweenness.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public class EdgeBetweennessGridSearch<U> implements EdgeMetricGridSearch<U>
{
    private final static String NORM = "norm";
    @Override
    public Map<String, Supplier<EdgeMetric<U>>> grid(Grid grid, DistanceCalculator<U> distCalc)
    {
        List<Boolean> normalize = grid.getBooleanValues(NORM);
        Map<String, Supplier<EdgeMetric<U>>> map = new HashMap<>();
        normalize.forEach(norm -> map.put(BETWEENNESS + "_" + (norm ? "norm" : "notnorm"), () -> new EdgeBetweenness<>(distCalc, norm)));
        return map;
    }

    @Override
    public Map<String, EdgeMetricFunction<U>> grid(Grid grid)
    {
        List<Boolean> normalize = grid.getBooleanValues(NORM);
        Map<String, EdgeMetricFunction<U>> map = new HashMap<>();
        normalize.forEach(norm -> map.put(BETWEENNESS + "_" + (norm ? "norm" : "notnorm"), (distCalc) -> new EdgeBetweenness<>(norm)));
        return map;
    }

}
