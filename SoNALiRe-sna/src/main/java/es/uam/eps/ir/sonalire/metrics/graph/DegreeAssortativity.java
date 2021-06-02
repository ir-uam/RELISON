/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.metrics.graph;

import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;
import es.uam.eps.ir.sonalire.metrics.vertex.Degree;

/**
 * Class for computing the degree assortativity in a graph.
 *
 * <p>
 * <b>Reference: </b> M.E.J. Newman. Assortative mixing in networks. Physical Review Letters 89(20), 208701 (2002)
 * </p>
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class DegreeAssortativity<U> extends Assortativity<U>
{
    /**
     * Constructor.
     *
     * @param dir the degree to take in the comparison.
     */
    public DegreeAssortativity(EdgeOrientation dir)
    {
        super((u, graph) ->
        {
            Degree<U> degree = new Degree<>(dir);
            return degree.compute(graph, u);
        });
    }

}
