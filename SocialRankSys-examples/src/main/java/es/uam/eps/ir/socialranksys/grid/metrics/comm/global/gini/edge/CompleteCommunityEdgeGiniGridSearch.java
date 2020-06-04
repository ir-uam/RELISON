/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.metrics.comm.global.gini.edge;

import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.metrics.comm.global.GlobalCommunityMetricGridSearch;
import es.uam.eps.ir.socialranksys.metrics.CommunityMetric;
import es.uam.eps.ir.socialranksys.metrics.communities.graph.gini.edges.CompleteCommunityEdgeGini;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.metrics.comm.global.GlobalCommunityMetricIdentifiers.COMPLETECOMMUNITYEDGEGINI;


/**
 * Grid for the complete community edge Gini of the graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class CompleteCommunityEdgeGiniGridSearch<U> implements GlobalCommunityMetricGridSearch<U>
{
    
    /**
     * Identifier for the variable that indicates if autoloops are allowed or not.
     */
    private final static String NODEAUTOLOOPS = "autoloops";
    @Override
    public Map<String, Supplier<CommunityMetric<U>>> grid(Grid grid)
    {
        Map<String, Supplier<CommunityMetric<U>>> metrics = new HashMap<>();
        grid.getBooleanValues(NODEAUTOLOOPS).forEach(autoloops -> 
            metrics.put(COMPLETECOMMUNITYEDGEGINI + "_" +  (autoloops ? "autoloops" : "noautoloops"), () -> new CompleteCommunityEdgeGini<>(autoloops)));
        
        return metrics;
    }
    
}