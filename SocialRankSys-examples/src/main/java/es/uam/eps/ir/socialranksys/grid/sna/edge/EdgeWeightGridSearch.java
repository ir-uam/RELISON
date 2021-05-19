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
import es.uam.eps.ir.socialranksys.metrics.edge.EdgeWeight;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.sna.edge.EdgeMetricIdentifiers.WEIGHT;


/**
 * Grid search for the edge weight.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public class EdgeWeightGridSearch<U> implements EdgeMetricGridSearch<U>
{
    @Override
    public Map<String, Supplier<EdgeMetric<U>>> grid(Grid grid, DistanceCalculator<U> distCalc)
    {
        Map<String, Supplier<EdgeMetric<U>>> map = new HashMap<>();
        map.put(WEIGHT, EdgeWeight::new);
        return map;
    }

    @Override
    public Map<String, EdgeMetricFunction<U>> grid(Grid grid)
    {
        Map<String, EdgeMetricFunction<U>> map = new HashMap<>();
        map.put(WEIGHT, (distcalc) -> new EdgeWeight<>());
        return map;
    }

}
