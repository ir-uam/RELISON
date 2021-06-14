/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.sna.graph;


import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.sna.metrics.GraphMetric;
import es.uam.eps.ir.relison.sna.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.relison.sna.metrics.distance.graph.ASL;
import es.uam.eps.ir.relison.sna.metrics.distance.modes.ASLMode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.relison.grid.sna.graph.GraphMetricIdentifiers.ASL;

/**
 * Grid for the average shortest path length of a graph.
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @param <U> type of the users
 *
 * @see ASL
 */
public class ASLGridSearch<U> implements GraphMetricGridSearch<U> 
{
    /**
     * Identifier for the computing mode
     */
    private final static String MODE = "mode";
    /**
     * Identifier for the harmonic mean mode
     */
    private final static String NONINFINITEDISTANCES = "Non infinite distances";
    /**
     * Identifier for the averaging over components
     */
    private final static String COMPONENTS = "Components";
       
    @Override
    public Map<String, Supplier<GraphMetric<U>>> grid(Grid grid, DistanceCalculator<U> distCalc)
    {
        Map<String, Supplier<GraphMetric<U>>> metrics = new HashMap<>();
        List<String> modes = grid.getStringValues(MODE);
        
        modes.forEach(mode ->
        {
            if(mode.equals(NONINFINITEDISTANCES))
            {
                metrics.put(ASL + "_" + NONINFINITEDISTANCES, () -> new ASL<>(distCalc, ASLMode.NONINFINITEDISTANCES));
            }
            else
            {
                metrics.put(ASL + "_" + COMPONENTS, () -> new ASL<>(distCalc, ASLMode.COMPONENTS));
            }
        });
        
        return metrics;
    }
    
    @Override
    public Map<String, GraphMetricFunction<U>> grid(Grid grid) 
    {
        Map<String, GraphMetricFunction<U>> metrics = new HashMap<>();
        List<String> modes = grid.getStringValues(MODE);
        
        modes.forEach(mode -> 
        {
            if(mode.equals(NONINFINITEDISTANCES))
            {
                metrics.put(ASL + "_" + NONINFINITEDISTANCES, (distCalc) -> new ASL<>(distCalc, ASLMode.NONINFINITEDISTANCES));
            }
            else
            {
                metrics.put(ASL + "_" + COMPONENTS, (distCalc) -> new ASL<>(distCalc, ASLMode.COMPONENTS));
            }
        });
        
        return metrics;
    } 
}
