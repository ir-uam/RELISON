/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.sna.vertex;

import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.metrics.VertexMetric;
import es.uam.eps.ir.relison.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.relison.metrics.vertex.LocalClusteringCoefficient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.relison.grid.sna.vertex.VertexMetricIdentifiers.LOCALCLUSTCOEF;

/**
 * Grid for the local clustering coefficient of a node.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 *
 * @see LocalClusteringCoefficient
 */
public class LocalClusteringCoefficientGridSearch<U> implements VertexMetricGridSearch<U> 
{
    /**
     * Identifier for the first neighbor of the target user.
     */
    private final static String VSEL = "vSel";
    /**
     * Identifier for the second neighbor of the target user.
     */
    private final static String WSEL = "wSel";

    @Override
    public Map<String, Supplier<VertexMetric<U>>> grid(Grid grid, DistanceCalculator<U> distCalc)
    {
        Map<String, Supplier<VertexMetric<U>>> metrics = new HashMap<>();
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        List<EdgeOrientation> wSels = grid.getOrientationValues(WSEL);
        
        vSels.forEach(vSel ->
            wSels.forEach(wSel ->
                metrics.put(LOCALCLUSTCOEF + "_" + vSel + "_" + wSel, () ->
                    new LocalClusteringCoefficient<>(vSel, wSel))));

        return metrics;
    }

    @Override
    public Map<String, VertexMetricFunction<U>> grid(Grid grid)
    {
        Map<String, VertexMetricFunction<U>> metrics = new HashMap<>();
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        List<EdgeOrientation> wSels = grid.getOrientationValues(WSEL);
        
        wSels.forEach(vSel ->
            wSels.forEach(wSel ->
                metrics.put(LOCALCLUSTCOEF + "_" + vSel + "_" + wSel, (distCalc) ->
                    new LocalClusteringCoefficient<>(vSel, wSel))));

        return metrics;
    }
    
}
