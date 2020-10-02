/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph.complementary;

import es.uam.eps.ir.socialranksys.graph.DirectedGraph;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.UnweightedGraph;

/**
 * Directed unweighted complementary graph.
 *
 * @param <U> Type of the vertices.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class DirectedUnweightedComplementaryGraph<U> extends ComplementaryGraph<U> implements DirectedGraph<U>, UnweightedGraph<U>
{
    /**
     * Constructor.
     *
     * @param graph Original graph.
     */
    public DirectedUnweightedComplementaryGraph(Graph<U> graph)
    {
        super(graph);
    }
}
