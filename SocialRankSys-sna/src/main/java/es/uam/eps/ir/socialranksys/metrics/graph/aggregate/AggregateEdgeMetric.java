/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package es.uam.eps.ir.socialranksys.metrics.graph.aggregate;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.metrics.EdgeMetric;
import es.uam.eps.ir.socialranksys.metrics.GraphMetric;

/**
 * Aggregate edge metric
 * @author Javier Sanz-Cruzado Puig (javier.sanz-cruzado@uam.es)
 * @param <U> type of the users.
 */
public class AggregateEdgeMetric<U> implements GraphMetric<U>
{
    /**
     * Edge metric
     */
    private final EdgeMetric<U> edgeMetric;
    
    /**
     * Constructor.
     * @param edgeMetric Vertex metric.
     */
    public AggregateEdgeMetric(EdgeMetric<U> edgeMetric)
    {
        this.edgeMetric = edgeMetric;
    }
    
    @Override
    public double compute(Graph<U> graph)
    {
        return edgeMetric.averageValue(graph);
    }

}
