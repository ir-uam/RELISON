/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.sna.graph;

import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;
import es.uam.eps.ir.sonalire.grid.Grid;
import es.uam.eps.ir.sonalire.metrics.GraphMetric;
import es.uam.eps.ir.sonalire.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.sonalire.metrics.graph.ClusteringCoefficientComplement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.sonalire.grid.sna.graph.GraphMetricIdentifiers.CLUSTCOEFCOMPL;


/**
 * Grid for the complement of the clustering coefficient of a graph.
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @param <U> type of the users
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
