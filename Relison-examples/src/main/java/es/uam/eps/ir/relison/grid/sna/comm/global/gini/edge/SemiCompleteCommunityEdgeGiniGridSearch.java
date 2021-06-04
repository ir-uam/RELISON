/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.sna.comm.global.gini.edge;

import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.grid.sna.comm.global.GlobalCommunityMetricGridSearch;
import es.uam.eps.ir.relison.metrics.CommunityMetric;
import es.uam.eps.ir.relison.metrics.communities.graph.gini.edges.SemiCompleteCommunityEdgeGini;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.relison.grid.sna.comm.global.GlobalCommunityMetricIdentifiers.SEMICOMPLETECOMMUNITYEDGEGINI;

/**
 * Grid for the semi-complete community edge Gini of the graph.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 */
public class SemiCompleteCommunityEdgeGiniGridSearch<U> implements GlobalCommunityMetricGridSearch<U>
{
    /**
     * Identifier for the variable that indicates if selfloops are allowed or not.
     */
    private final static String NODESELFLOOPS = "selfloops";

    @Override
    public Map<String, Supplier<CommunityMetric<U>>> grid(Grid grid)
    {
        Map<String, Supplier<CommunityMetric<U>>> metrics = new HashMap<>();
        grid.getBooleanValues(NODESELFLOOPS).forEach(selfloops ->
            metrics.put(SEMICOMPLETECOMMUNITYEDGEGINI + "_" + (selfloops ? "selfloops" : "noselfloops"), () -> new SemiCompleteCommunityEdgeGini<>(selfloops)));
        
        return metrics;
    }
    
}
