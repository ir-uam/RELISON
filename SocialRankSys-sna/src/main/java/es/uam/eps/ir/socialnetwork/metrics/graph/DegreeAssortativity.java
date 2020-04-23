/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.metrics.graph;

import es.uam.eps.ir.socialnetwork.metrics.vertex.Degree;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;

/**
 * Computes the degree assortativity for a graph.
 * 
 * Newman, M.E.J. Assortative mixing in networks. Physical Review Letters 89(20), 208701 (2002)
 * 
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public class DegreeAssortativity<U> extends Assortativity<U> 
{
    /**
     * Constructor
     * @param dir the degree to take in the comparison
     */
    public DegreeAssortativity(EdgeOrientation dir)
    {
        super((u, graph) -> {
            Degree<U> degree = new Degree<>(dir);
            return degree.compute(graph,u);
        });
    }
    
}
