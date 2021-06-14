/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.sna.comm.global;

import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.sna.metrics.CommunityMetric;
import es.uam.eps.ir.relison.sna.metrics.communities.graph.WeakTies;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;


/**
 * Grid for the number of weak ties of the graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class WeakTiesGridSearch<U> implements GlobalCommunityMetricGridSearch<U> 
{
    
    @Override
    public Map<String, Supplier<CommunityMetric<U>>> grid(Grid grid)
    {
        Map<String, Supplier<CommunityMetric<U>>> metrics = new HashMap<>();

        metrics.put(GlobalCommunityMetricIdentifiers.WEAKTIES, WeakTies::new);
        
        return metrics;
    }
    
}
