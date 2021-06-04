/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.metrics.graph;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.metrics.GraphMetric;

/**
 * Computes the number of edges in the graph.
 *
 * @param <U> type of the vertices.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class NumEdges<U> implements GraphMetric<U>
{

    @Override
    public double compute(Graph<U> graph)
    {
        return graph.getEdgeCount() + 0.0;
    }

}
