/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.sna.comm.indiv;

import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.metrics.IndividualCommunityMetric;
import es.uam.eps.ir.socialranksys.metrics.communities.indiv.Size;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.sna.comm.indiv.IndividualCommunityMetricIdentifiers.COMMSIZE;


/**
 * Grid for the degree of a node.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class CommSizeGridSearch<U> implements IndividualCommunityMetricGridSearch<U> 
{    
    @Override
    public Map<String, Supplier<IndividualCommunityMetric<U>>> grid(Grid grid)
    {
        Map<String, Supplier<IndividualCommunityMetric<U>>> metrics = new HashMap<>();
        metrics.put(COMMSIZE, Size::new);
       
        return metrics;
    }
    
}
