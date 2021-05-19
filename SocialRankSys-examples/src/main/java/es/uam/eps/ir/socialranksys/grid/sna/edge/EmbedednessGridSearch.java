/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.sna.edge;

import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.metrics.EdgeMetric;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.socialranksys.metrics.edge.Embededness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.sna.edge.EdgeMetricIdentifiers.EMBEDEDNESS;

/**
 * Grid for the embeddedness of an edge.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class EmbedednessGridSearch<U> implements EdgeMetricGridSearch<U> 
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
    public Map<String, Supplier<EdgeMetric<U>>> grid(Grid grid, DistanceCalculator<U> distCalc)
    {
        Map<String, Supplier<EdgeMetric<U>>> metrics = new HashMap<>();
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        
        uSels.forEach(uSel ->
            vSels.forEach(vSel ->
                metrics.put(EMBEDEDNESS + "_" + uSel + "_" + vSel, () -> new Embededness<>(uSel, vSel))));

        return metrics;
    }

    @Override
    public Map<String, EdgeMetricFunction<U>> grid(Grid grid)
    {
        Map<String, EdgeMetricFunction<U>> metrics = new HashMap<>();
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        
        uSels.forEach(uSel ->
            vSels.forEach(vSel ->
                metrics.put(EMBEDEDNESS + "_" + uSel + "_" + vSel, (distcalc) -> new Embededness<>(uSel, vSel))));

        return metrics;
    } 
}
