/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.metrics.graph;

import es.uam.eps.ir.socialnetwork.metrics.GraphMetric;
import es.uam.eps.ir.socialranksys.graph.Graph;

/**
 * Computes the number of edges in the graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the vertices.
 */
public class NumEdges<U> implements GraphMetric<U>
{

    @Override
    public double compute(Graph<U> graph)
    {
        return graph.getEdgeCount() + 0.0;
    }
    
}
