/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.metrics.graph;

import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.metrics.vertex.Degree;

/**
 * Computes the degree Pearson correlation for the links in a graph.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class DegreePearsonCorrelation<U> extends PearsonCorrelation<U>
{
    /**
     * Constructor.
     *
     * @param uSel Degree selection for the origin node.
     * @param vSel Degree selection for the destination node.
     */
    public DegreePearsonCorrelation(EdgeOrientation uSel, EdgeOrientation vSel)
    {
        super((u, graph) ->
        {
          Degree<U> degree = new Degree<>(uSel);
          return degree.compute(graph, u);
        },
        (u, graph) ->
        {
            Degree<U> degree = new Degree<>(vSel);
            return degree.compute(graph, u);
        });
    }
}
