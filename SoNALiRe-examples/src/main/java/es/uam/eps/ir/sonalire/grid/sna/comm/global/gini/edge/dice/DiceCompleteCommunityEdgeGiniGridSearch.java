/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.sna.comm.global.gini.edge.dice;

import es.uam.eps.ir.sonalire.grid.Grid;
import es.uam.eps.ir.sonalire.grid.sna.comm.global.GlobalCommunityMetricGridSearch;
import es.uam.eps.ir.sonalire.metrics.CommunityMetric;
import es.uam.eps.ir.sonalire.metrics.communities.graph.gini.edges.dice.DiceCompleteCommunityEdgeGini;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.sonalire.grid.sna.comm.global.GlobalCommunityMetricIdentifiers.DICECOMPLETECOMMUNITYEDGEGINI;

/**
 * Grid for the Dice Complete Community Edge Gini metric.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public class DiceCompleteCommunityEdgeGiniGridSearch<U> implements GlobalCommunityMetricGridSearch<U>
{

    /**
     * Identifier for the autoloop selection
     */
    private static final String AUTOLOOPS = "autoloops";
    @Override
    public Map<String, Supplier<CommunityMetric<U>>> grid(Grid grid)
    {
        Map<String, Supplier<CommunityMetric<U>>> metrics = new HashMap<>();
        List<Boolean> loops = grid.getBooleanValues(AUTOLOOPS);
        

        loops.forEach(loop ->
            metrics.put(DICECOMPLETECOMMUNITYEDGEGINI + "_" + (loop ? "autoloops" : "noautoloops"), () -> new DiceCompleteCommunityEdgeGini<>(loop)));

        return metrics;
    }
    
}
