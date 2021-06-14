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
import es.uam.eps.ir.relison.sna.metrics.distance.vertex.Closeness;
import es.uam.eps.ir.relison.sna.metrics.distance.vertex.HarmonicCentrality;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Grid for the harmonic centrality of a node.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 *
 * @see Closeness
 * @see HarmonicCentrality
 */
public class HarmonicCentralityGridSearch<U> implements VertexMetricGridSearch<U>
{
    @Override
    public Map<String, Supplier<VertexMetric<U>>> grid(Grid grid, DistanceCalculator<U> distCalc)
    {
        Map<String, Supplier<VertexMetric<U>>> metrics = new HashMap<>();
        metrics.put(VertexMetricIdentifiers.HARMONIC, () -> new HarmonicCentrality<>(distCalc));
        return metrics;
    }

    @Override
    public Map<String, VertexMetricFunction<U>> grid(Grid grid)
    {
        Map<String, VertexMetricFunction<U>> metrics = new HashMap<>();
        metrics.put(VertexMetricIdentifiers.HARMONIC, HarmonicCentrality::new);
        return metrics;
    }
    
}
