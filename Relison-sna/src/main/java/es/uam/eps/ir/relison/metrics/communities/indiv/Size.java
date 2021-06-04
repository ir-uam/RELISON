/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.metrics.communities.indiv;

import es.uam.eps.ir.relison.community.Communities;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.metrics.IndividualCommunityMetric;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

import java.util.Map;
import java.util.OptionalDouble;

/**
 * Computes the size of communities.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Size<U> implements IndividualCommunityMetric<U>
{

    @Override
    public double compute(Graph<U> graph, Communities<U> comm, int indiv)
    {
        return comm.getUsers(indiv).count() + 0.0;
    }

    @Override
    public Map<Integer, Double> compute(Graph<U> graph, Communities<U> comm)
    {
        Map<Integer, Double> map = new Int2DoubleOpenHashMap();
        comm.getCommunities().forEach(c -> map.put(c, this.compute(graph, comm, c)));
        return map;
    }

    @Override
    public double averageValue(Graph<U> graph, Communities<U> comm)
    {
        OptionalDouble optional = this.compute(graph, comm).values().stream().mapToDouble(value -> value).average();
        return optional.isPresent() ? optional.getAsDouble() : 0.0;
    }

}
