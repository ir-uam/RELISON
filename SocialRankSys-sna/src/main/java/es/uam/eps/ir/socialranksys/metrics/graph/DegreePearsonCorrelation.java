/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.graph;

import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.metrics.vertex.Degree;

/**
 * Computes the degree Pearson correlation for the links in a graph
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public class DegreePearsonCorrelation<U> extends PearsonCorrelation<U> 
{
    /**
     * Constructor
     * @param uSel Degree selection for the origin node.
     * @param vSel Degree selection for the destination node.
     */
    public DegreePearsonCorrelation(EdgeOrientation uSel, EdgeOrientation vSel)
    {
        super((u, graph) -> 
        {
            Degree<U> degree = new Degree<>(uSel);
            return degree.compute(graph,u);
        },
        (u, graph) -> 
        {
            Degree<U> degree = new Degree<>(vSel);
            return degree.compute(graph,u);
        });
    }
    
}
