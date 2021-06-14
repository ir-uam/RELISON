/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.sna.graph;


import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.sna.metrics.GraphMetric;
import es.uam.eps.ir.relison.sna.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.relison.sna.metrics.graph.DegreePearsonCorrelation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.relison.grid.sna.graph.GraphMetricIdentifiers.DEGREEPEARSON;


/**
 * Grid search for the degree Pearson correlation metric
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @param <U> type of the users
 *
 * @see DegreePearsonCorrelation
 */
public class DegreePearsonCorrelationGridSearch<U> implements GraphMetricGridSearch<U> {
    /**
     * Identifier for the first degree selection
     */
    private static final String USEL = "uSel";
    /**
     * Identifier for the second degree selection
     */
    private static final String VSEL = "vSel";

    @Override
    public Map<String, Supplier<GraphMetric<U>>> grid(Grid grid, DistanceCalculator<U> distCalc)
    {
        Map<String, Supplier<GraphMetric<U>>> metrics = new HashMap<>();
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);

        uSels.forEach(uSel ->
            vSels.forEach(vSel ->
                metrics.put(DEGREEPEARSON + "_" + uSel + "_" + vSel, () -> new DegreePearsonCorrelation<>(uSel, vSel))));
        
        return metrics;
    }

    @Override
    public Map<String, GraphMetricFunction<U>> grid(Grid grid)
    {
        Map<String, GraphMetricFunction<U>> metrics = new HashMap<>();
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);

        uSels.forEach(uSel -> 
            vSels.forEach(vSel ->
                metrics.put(DEGREEPEARSON + "_" + uSel + "_" + vSel, (distCalc) -> new DegreePearsonCorrelation<>(uSel, vSel))));
        
        return metrics;
    }
}
