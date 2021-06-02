/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.sna.comm.global.gini.degree;

import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;
import es.uam.eps.ir.sonalire.grid.Grid;
import es.uam.eps.ir.sonalire.grid.sna.comm.global.GlobalCommunityMetricGridSearch;
import es.uam.eps.ir.sonalire.grid.sna.comm.global.GlobalCommunityMetricIdentifiers;
import es.uam.eps.ir.sonalire.metrics.CommunityMetric;
import es.uam.eps.ir.sonalire.metrics.communities.graph.gini.degree.InterCommunityDegreeGini;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Grid for the inter-community degree Gini of the graph.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 *
 * @see InterCommunityDegreeGini
 */
public class InterCommunityDegreeGiniGridSearch<U> implements GlobalCommunityMetricGridSearch<U>
{
    /**
     * Identifier for the degree selection
     */
    private static final String ORIENT = "orientation";
    
    @Override
    public Map<String, Supplier<CommunityMetric<U>>> grid(Grid grid)
    {
        Map<String, Supplier<CommunityMetric<U>>> metrics = new HashMap<>();
        List<EdgeOrientation> orients = grid.getOrientationValues(ORIENT);
        
        orients.forEach(orient ->
            metrics.put(GlobalCommunityMetricIdentifiers.INTERCOMMUNITYDEGREEGINI + "_" + orient, () -> new InterCommunityDegreeGini<>(orient)));

        return metrics;
    }
    
}
