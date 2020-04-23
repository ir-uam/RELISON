/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.metrics.communities.graph.gini.size;

import es.uam.eps.ir.socialnetwork.metrics.CommunityMetric;
import es.uam.eps.ir.socialnetwork.metrics.communities.indiv.Size;
import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.utils.indexes.GiniIndex;

import java.util.Map;

/**
 * Computes the Gini coefficient of the distribution of community sizes.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class CommunitySizeGini<U> implements CommunityMetric<U> 
{
    @Override
    public double compute(Graph<U> graph, Communities<U> comm)
    {
        Size<U> commSize = new Size<>();
        Map<Integer, Double> sizes = commSize.compute(graph, comm);
        
        GiniIndex gini = new GiniIndex();
        return 1.0 - gini.compute(sizes.values().stream(), false);
    }
    
}
