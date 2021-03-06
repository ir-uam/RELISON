/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.sna.comm.global.gini.degree.sizenormalized;

import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.grid.sna.comm.global.GlobalCommunityMetricGridSearch;
import es.uam.eps.ir.relison.grid.sna.comm.global.GlobalCommunityMetricIdentifiers;
import es.uam.eps.ir.relison.sna.metrics.CommunityMetric;
import es.uam.eps.ir.relison.sna.metrics.communities.graph.gini.degree.sizenormalized.SizeNormalizedInterCommunityDegreeGini;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;


/**
 * Grid for the size normalized inter-community degree Gini of the graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class SizeNormalizedInterCommunityDegreeGiniGridSearch<U> implements GlobalCommunityMetricGridSearch<U>
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
        
        orients.forEach(orient -> metrics.put(GlobalCommunityMetricIdentifiers.SIZENORMINTERCOMMUNITYDEGREEGINI + "_" + orient, () -> new SizeNormalizedInterCommunityDegreeGini<>(orient)));
        return metrics;
    }
    
}
