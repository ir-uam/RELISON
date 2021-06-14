/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.sna.metrics.communities.indiv;

import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.sna.metrics.IndividualCommunityMetric;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * Computes the volume of the community: the sum of the degrees
 * of the nodes in the community.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Volume<U> implements IndividualCommunityMetric<U>
{

    /**
     * Indicates if the degree to use is inDegree, outDegree or the
     * full degree of the community graph.
     */
    private final EdgeOrientation orientation;

    /**
     * Constructor.
     *
     * @param orientation indicates if the degree to use is the in-degree, out-degree or full
     *                    full degree of the nodes in the community graph.
     */
    public Volume(EdgeOrientation orientation)
    {
        this.orientation = orientation;
    }

    @Override
    public double compute(Graph<U> graph, Communities<U> comm, int indiv)
    {
        return comm.getUsers(indiv).mapToDouble(u -> graph.degree(u, orientation)).sum();
    }

    @Override
    public Map<Integer, Double> compute(Graph<U> graph, Communities<U> comm)
    {
        Map<Integer, Double> map = new HashMap<>();
        comm.getCommunities().forEach(indiv ->
            map.put(indiv, comm.getUsers(indiv).mapToDouble(u -> graph.degree(u, orientation)).sum()));

        return map;
    }

    @Override
    public double averageValue(Graph<U> graph, Communities<U> comm)
    {
        OptionalDouble opt = this.compute(graph, comm).values().stream().mapToDouble(x -> x).average();
        return opt.isPresent() ? opt.getAsDouble() : 0.0;
    }


}
