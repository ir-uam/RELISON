/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.sna.comm.indiv;

import es.uam.eps.ir.sonalire.grid.Grid;
import es.uam.eps.ir.sonalire.metrics.IndividualCommunityMetric;
import es.uam.eps.ir.sonalire.metrics.communities.indiv.Size;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;


/**
 * Grid for the size of a community.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 *
 * @see Size
 */
public class CommSizeGridSearch<U> implements IndividualCommunityMetricGridSearch<U> 
{    
    @Override
    public Map<String, Supplier<IndividualCommunityMetric<U>>> grid(Grid grid)
    {
        Map<String, Supplier<IndividualCommunityMetric<U>>> metrics = new HashMap<>();
        metrics.put(IndividualCommunityMetricIdentifiers.COMMSIZE, Size::new);
       
        return metrics;
    }
    
}
