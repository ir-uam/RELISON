/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.metrics.vertex;


import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.metrics.VertexMetric;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.socialranksys.metrics.distance.modes.ClosenessMode;
import es.uam.eps.ir.socialranksys.metrics.distance.vertex.Closeness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.metrics.vertex.VertexMetricIdentifiers.CLOSENESS;


/**
 * Grid for the closeness of a node.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class ClosenessGridSearch<U> implements VertexMetricGridSearch<U> 
{
    /**
     * Identifier for the computing mode
     */
    private final static String MODE = "mode";
    /**
     * Identifier for the harmonic mean mode
     */
    private final static String HARMONICMEAN = "Harmonic mean";
    /**
     * Identifier for the averaging over components
     */
    private final static String COMPONENTS = "Components";

    @Override
    public Map<String, Supplier<VertexMetric<U>>> grid(Grid grid, DistanceCalculator<U> distCalc)
    {
        Map<String, Supplier<VertexMetric<U>>> metrics = new HashMap<>();
        List<String> modes = grid.getStringValues(MODE);
        
        modes.forEach(mode -> 
        {
            if(mode.equals(HARMONICMEAN))
            {
                metrics.put(CLOSENESS + "_" + HARMONICMEAN, () -> new Closeness<>(distCalc, ClosenessMode.HARMONICMEAN));
            }
            else
            {
                metrics.put(CLOSENESS + "_" + COMPONENTS, () -> new Closeness<>(distCalc, ClosenessMode.COMPONENTS));
            }
        });
        
        return metrics;
    }

    @Override
    public Map<String, VertexMetricFunction<U>> grid(Grid grid)
    {
        Map<String, VertexMetricFunction<U>> metrics = new HashMap<>();
        List<String> modes = grid.getStringValues(MODE);
        
        modes.forEach(mode -> 
        {
            if(mode.equals(HARMONICMEAN))
            {
                metrics.put(CLOSENESS + "_" + HARMONICMEAN, (distCalc) -> new Closeness<>(distCalc, ClosenessMode.HARMONICMEAN));
            }
            else
            {
                metrics.put(CLOSENESS + "_" + COMPONENTS, (distCalc) -> new Closeness<>(distCalc, ClosenessMode.COMPONENTS));
            }
        });
        
        return metrics;
    }
    
}
