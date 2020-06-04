/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.metrics.pair;

import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.metrics.PairMetric;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.socialranksys.metrics.pair.ExpandedNeighborCountedOverlap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.metrics.pair.PairMetricIdentifiers.EFOAFCOUNT;


/**
 * Grid for the embeddedness of an edge.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class ExpandedFOAFCountGridSearch<U> implements PairMetricGridSearch<U> 
{    

    /**
     * Identifier for the origin neighborhood selection
     */
    private final static String USEL = "uSel";
    /**
     * Identifier for the destination neighborhood selection
     */
    private final static String VSEL = "vSel";
    
    private final static String ORIGIN = "origin";

    @Override
    public Map<String, Supplier<PairMetric<U>>> grid(Grid grid, DistanceCalculator<U> distCalc)
    {
        Map<String, Supplier<PairMetric<U>>> metrics = new HashMap<>();
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        List<Boolean> origins = grid.getBooleanValues(ORIGIN);
        
        origins.forEach(origin ->
            uSels.forEach(uSel ->
                vSels.forEach(vSel ->
                    metrics.put(EFOAFCOUNT + (origin ? "_origin" : "_dest" )+ "_" + uSel + "_" + vSel, () ->
                        new ExpandedNeighborCountedOverlap<>(origin, uSel, vSel)
                    )
                )
            )
        );

        return metrics;
    }

    @Override
    public Map<String, PairMetricFunction<U>> grid(Grid grid)
    {
        Map<String, PairMetricFunction<U>> metrics = new HashMap<>();
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        List<Boolean> origins = grid.getBooleanValues(ORIGIN);

        origins.forEach(origin ->
            uSels.forEach(uSel ->
                vSels.forEach(vSel ->
                    metrics.put(EFOAFCOUNT + (origin ? "_origin" : "_dest" ) + "_" + uSel + "_" + vSel, (distCalc) ->
                        new ExpandedNeighborCountedOverlap<>(origin, uSel, vSel)
                    )
                )
            )
        );

        return metrics;
    }
    
}