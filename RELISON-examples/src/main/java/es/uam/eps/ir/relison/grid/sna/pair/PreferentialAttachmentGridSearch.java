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
import es.uam.eps.ir.relison.sna.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.relison.sna.metrics.pair.PreferentialAttachment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Grid for the preferential attachment of a pair of users.
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @param <U> type of the users
 *
 * @see PreferentialAttachment
 */
public class PreferentialAttachmentGridSearch<U> implements PairMetricGridSearch<U> 
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
                metrics.put(PairMetricIdentifiers.PREFATTACH + "_" + uSel + "_" + vSel, () -> new PreferentialAttachment<>(uSel, vSel)))
        );

        return metrics;
    }

    @Override
    public Map<String, PairMetricFunction<U>> grid(Grid grid)
    {
        Map<String, PairMetricFunction<U>> metrics = new HashMap<>();
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        
        uSels.forEach(uSel ->
            vSels.forEach(vSel -> metrics.put(PairMetricIdentifiers.PREFATTACH + "_" + uSel + "_" + vSel, (distCalc) -> new PreferentialAttachment<>(uSel, vSel)))
        );

        return metrics;
    }
    
}
