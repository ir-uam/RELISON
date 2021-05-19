/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.sna.graph;

import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.metrics.GraphMetric;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.socialranksys.metrics.graph.ClusteringCoefficientComplement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.sna.graph.GraphMetricIdentifiers.CLUSTCOEFCOMPL;


/**
 * Grid for the clustering coefficient of a graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class ClusteringCoefficientComplementGridSearch<U> implements GraphMetricGridSearch<U>
{    
    /**
     * Identifier for the node first neighbor selection
     */
    private final static String USEL = "uSel";
    /**
     * Identifier for the node second neighbor selection
     */
    private final static String VSEL = "vSel";

    @Override
    public Map<String, Supplier<GraphMetric<U>>> grid(Grid grid, DistanceCalculator<U> distCalc)
    {
        Map<String, Supplier<GraphMetric<U>>> metrics = new HashMap<>();
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        
        uSels.forEach(uSel ->
            vSels.forEach(vSel ->
               metrics.put(CLUSTCOEFCOMPL + "_" + uSel + "_" + vSel, () -> new ClusteringCoefficientComplement<>(uSel, vSel))));
        
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
               metrics.put(CLUSTCOEFCOMPL + "_" + uSel + "_" + vSel, (distcalc) -> new ClusteringCoefficientComplement<>(uSel, vSel))));
        
        return metrics;
    }
    
}
