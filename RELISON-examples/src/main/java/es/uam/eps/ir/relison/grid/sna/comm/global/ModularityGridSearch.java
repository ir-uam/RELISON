/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.sna.comm.global;

import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.sna.metrics.CommunityMetric;
import es.uam.eps.ir.relison.sna.metrics.communities.graph.Modularity;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;


/**
 * Grid for the modularity of the graph.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 *
 * @see Modularity
 */
public class ModularityGridSearch<U> implements GlobalCommunityMetricGridSearch<U> 
{
    
    @Override
    public Map<String, Supplier<CommunityMetric<U>>> grid(Grid grid)
    {
        Map<String, Supplier<CommunityMetric<U>>> metrics = new HashMap<>();

        metrics.put(GlobalCommunityMetricIdentifiers.MODULARITY, Modularity::new);
        
        return metrics;
    }
    
}
