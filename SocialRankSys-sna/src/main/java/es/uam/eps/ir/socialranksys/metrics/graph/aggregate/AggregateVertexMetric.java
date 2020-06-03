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
import es.uam.eps.ir.socialranksys.metrics.GraphMetric;
import es.uam.eps.ir.socialranksys.metrics.VertexMetric;

/**
 * Aggregate vertex metric.
 * @author Javier Sanz-Cruzado Puig (javier.sanz-cruzado@uam.es)
 * @param <U> Type of the users.
 */
public class AggregateVertexMetric<U> implements GraphMetric<U>
{
    /**
     * Vertex metric
     */
    private final VertexMetric<U> vertexMetric;
    
    /**
     * Constructor.
     * @param vertexMetric Vertex metric.
     */
    public AggregateVertexMetric(VertexMetric<U> vertexMetric)
    {
        this.vertexMetric = vertexMetric;
    }
    
    @Override
    public double compute(Graph<U> graph)
    {
        return vertexMetric.averageValue(graph);
    }

}
