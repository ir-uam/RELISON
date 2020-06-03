/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.communities.graph;

import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.metrics.CommunityMetric;

/**
 * Computes the average size of the destiny communities of the links.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public class CommunityDestinySize<U> implements CommunityMetric<U>
{
    @Override
    public double compute(Graph<U> graph, Communities<U> comm)
    {
        double value = graph.getAllNodes().mapToDouble(u -> graph.getAdjacentNodes(u)
               .filter(v -> comm.getCommunity(u) != comm.getCommunity(v))
               .mapToDouble(v -> comm.getUsers(comm.getCommunity(v)).count() + 0.0)
               .sum()).sum();
        
        CommunityMetric<U> wt = new WeakTies<>();
        double wtval = wt.compute(graph, comm);
        
        return value / wtval;
    }
    
}
