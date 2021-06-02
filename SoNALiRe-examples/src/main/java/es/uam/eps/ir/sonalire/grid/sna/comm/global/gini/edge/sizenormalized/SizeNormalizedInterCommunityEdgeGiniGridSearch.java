/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.sna.comm.global.gini.edge.sizenormalized;

import es.uam.eps.ir.sonalire.grid.Grid;
import es.uam.eps.ir.sonalire.grid.sna.comm.global.GlobalCommunityMetricGridSearch;
import es.uam.eps.ir.sonalire.metrics.CommunityMetric;
import es.uam.eps.ir.sonalire.metrics.communities.graph.gini.edges.sizenormalized.SizeNormalizedInterCommunityEdgeGini;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.sonalire.grid.sna.comm.global.GlobalCommunityMetricIdentifiers.SIZENORMINTERCOMMUNITYEDGEGINI;

/**
 * Grid for the size-normalized inter-community edge Gini of the graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class SizeNormalizedInterCommunityEdgeGiniGridSearch<U> implements GlobalCommunityMetricGridSearch<U>
{
    @Override
    public Map<String, Supplier<CommunityMetric<U>>> grid(Grid grid)
    {
        Map<String, Supplier<CommunityMetric<U>>> metrics = new HashMap<>();
        metrics.put(SIZENORMINTERCOMMUNITYEDGEGINI, SizeNormalizedInterCommunityEdgeGini::new);
        
        return metrics;
    }
    
}
