/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.metrics.vertex;

import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;
import es.uam.eps.ir.sonalire.metrics.VertexMetric;

import java.util.HashSet;
import java.util.Set;

/**
 * Metric that finds the number of different neighbors at distance 2 from a user.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Distance2Degree<U> implements VertexMetric<U>
{
    /**
     * The orientation for selecting the neighborhood of the user.
     */
    private final EdgeOrientation first;
    /**
     * The orientation for selecting the neighborhood of the user's neighbors.
     */
    private final EdgeOrientation second;

    /**
     * Constructor.
     *
     * @param first  the orientation for selecting the neighborhood of the user.
     * @param second the orientation for selecting the neighborhood of the user's neighbors.
     */
    public Distance2Degree(EdgeOrientation first, EdgeOrientation second)
    {
        this.first = first;
        this.second = second.invertSelection();
    }

    @Override
    public double compute(Graph<U> graph, U user)
    {
        Set<U> d2neighs = new HashSet<>();
        graph.getNeighbourhood(user, first).forEach(neigh -> graph.getNeighbourhood(neigh, second).forEach(d2neighs::add));
        return d2neighs.size();
    }
}
