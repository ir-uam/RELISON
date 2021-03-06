/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.sna.pair;

import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.sna.metrics.PairMetric;
import es.uam.eps.ir.relison.sna.metrics.complementary.pair.ComplementaryEmbededness;
import es.uam.eps.ir.relison.sna.metrics.distance.DistanceCalculator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.relison.grid.sna.pair.PairMetricIdentifiers.COMPLEMBEDEDNESS;


/**
 * Grid for the embeddedness of a pair in the complementary graph.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 */
public class ComplementaryEmbedednessGridSearch<U> implements PairMetricGridSearch<U> 
{    

    /**
     * Identifier for the origin neighborhood selection
     */
    private final static String USEL = "uSel";
    /**
     * Identifier for the destination neighborhood selection
     */
    private final static String VSEL = "vSel";

    @Override
    public Map<String, Supplier<PairMetric<U>>> grid(Grid grid, DistanceCalculator<U> distCalc)
    {
        Map<String, Supplier<PairMetric<U>>> metrics = new HashMap<>();
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        
        uSels.forEach(uSel ->
            vSels.forEach(vSel ->
                metrics.put(COMPLEMBEDEDNESS + "_" + uSel + "_" + vSel, () -> new ComplementaryEmbededness<>(uSel, vSel))));

        return metrics;
    }

    @Override
    public Map<String, PairMetricFunction<U>> grid(Grid grid)
    {
        Map<String, PairMetricFunction<U>> metrics = new HashMap<>();
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        
        uSels.forEach(uSel ->
            vSels.forEach(vSel ->
                metrics.put(COMPLEMBEDEDNESS + "_" + uSel + "_" + vSel, (distCalc) -> new ComplementaryEmbededness<>(uSel, vSel))));

        return metrics;
    }
    
}
