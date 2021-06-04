/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.metrics.communities.graph;

import es.uam.eps.ir.relison.community.Communities;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.metrics.CommunityMetric;

/**
 * Computes the average size of the destiny communities of the links.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
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
