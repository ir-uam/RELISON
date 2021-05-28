/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package es.uam.eps.ir.socialranksys.metrics.graph.aggregate;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.metrics.GraphMetric;
import es.uam.eps.ir.socialranksys.metrics.PairMetric;

/**
 * Graph metric computed as the aggregation of an edge metric over the edges in the network.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class AggregateEdgeMetric<U> implements GraphMetric<U>
{
    /**
     * Edge metric.
     */
    private final PairMetric<U> edgeMetric;

    /**
     * Constructor.
     *
     * @param edgeMetric Vertex metric.
     */
    public AggregateEdgeMetric(PairMetric<U> edgeMetric)
    {
        this.edgeMetric = edgeMetric;
    }

    @Override
    public double compute(Graph<U> graph)
    {
        return edgeMetric.averageValueOnlyLinks(graph);
    }

}
