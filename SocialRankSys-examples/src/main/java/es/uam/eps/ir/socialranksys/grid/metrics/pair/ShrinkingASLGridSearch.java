/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.metrics.pair;

import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.metrics.PairMetric;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.socialranksys.metrics.distance.pair.ShrinkingASL;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.metrics.pair.PairMetricIdentifiers.SHRINKINGASL;

/**
 *
 * @author Javier
 */
public class ShrinkingASLGridSearch<U> implements PairMetricGridSearch<U> {
    @Override
    public Map<String, Supplier<PairMetric<U>>> grid(Grid grid, DistanceCalculator<U> distCalc)
    {
        Map<String, Supplier<PairMetric<U>>> map = new HashMap<>();
        map.put(SHRINKINGASL, () -> new ShrinkingASL<>(distCalc));
        return map;
    }

    @Override
    public Map<String, PairMetricFunction<U>> grid(Grid grid)
    {
        Map<String, PairMetricFunction<U>> map = new HashMap<>();
        map.put(SHRINKINGASL, ShrinkingASL::new);
        return map;
    }
}