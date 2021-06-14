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
import es.uam.eps.ir.relison.sna.metrics.distance.pair.Distance;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;


/**
 * Grid search for the distance.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 *
 * @see Distance
 */
public class DistanceGridSearch<U> implements PairMetricGridSearch<U>
{
    @Override
    public Map<String, Supplier<PairMetric<U>>> grid(Grid grid, DistanceCalculator<U> distCalc)
    {
        Map<String, Supplier<PairMetric<U>>> map = new HashMap<>();
        map.put(PairMetricIdentifiers.DISTANCE, () -> new Distance<>(distCalc));
        return map;
    }

    @Override
    public Map<String, PairMetricFunction<U>> grid(Grid grid)
    {
        Map<String, PairMetricFunction<U>> map = new HashMap<>();
        map.put(PairMetricIdentifiers.DISTANCE, Distance::new);
        return map;
    }

}
