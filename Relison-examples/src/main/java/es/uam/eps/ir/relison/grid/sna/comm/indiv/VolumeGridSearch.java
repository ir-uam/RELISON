/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.sna.comm.indiv;

import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.metrics.IndividualCommunityMetric;
import es.uam.eps.ir.relison.metrics.communities.indiv.Volume;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;


/**
 * Grid for the volume of a node.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the
 *
 * @see Volume
 */
public class VolumeGridSearch<U> implements IndividualCommunityMetricGridSearch<U>
{
    /**
     * Identifier for the orientation
     */
    private static final String ORIENT = "orientation";
    
    @Override
    public Map<String, Supplier<IndividualCommunityMetric<U>>> grid(Grid grid)
    {
        Map<String, Supplier<IndividualCommunityMetric<U>>> metrics = new HashMap<>();
        List<EdgeOrientation> orients = grid.getOrientationValues(ORIENT);
        
        orients.forEach(orient ->
            metrics.put(IndividualCommunityMetricIdentifiers.VOLUME + "_" + orient, () -> new Volume<>(orient)));
        
        return metrics;
    }
    
}
