/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.pair;

import es.uam.eps.ir.socialranksys.graph.Graph;

/**
 * Finds the weight of an edge in a graph.
 *
 * @param <V> Type of the users in the graph.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class EdgeWeight<V> extends AbstractPairMetric<V>
{
    /**
     * Constructor.
     */
    public EdgeWeight()
    {
    }

    @Override
    public double compute(Graph<V> graph, V orig, V dest)
    {
        if (graph.isMultigraph())
        {
            return Double.NaN;
        }

        if (graph.containsEdge(orig, dest))
        {
            return graph.getEdgeWeight(orig, dest);
        }
        else
        {
            return 0.0;
        }
    }
}
