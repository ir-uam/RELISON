/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.graph;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.metrics.GraphMetric;

/**
 * Reciprocity rate of the graph (proportion of reciprocal links)
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class ReciprocityRate<U> implements GraphMetric<U>
{
    @Override
    public double compute(Graph<U> graph)
    {
        if (!graph.isDirected())
        {
            return 1.0;
        }

        double num = graph.getAllNodes().mapToDouble(u -> graph.getAdjacentNodes(u).filter(v -> graph.containsEdge(v, u)).count() + 0.0).sum();
        double den = graph.getEdgeCount();
        return num / den;
    }

}
