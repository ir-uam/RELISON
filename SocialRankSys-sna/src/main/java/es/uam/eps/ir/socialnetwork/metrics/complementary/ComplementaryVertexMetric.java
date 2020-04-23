/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.metrics.complementary;

import es.uam.eps.ir.socialnetwork.metrics.VertexMetric;
import es.uam.eps.ir.socialranksys.graph.Graph;

/**
 * Computes a vertex metric over the complementary graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public class ComplementaryVertexMetric<U> implements VertexMetric<U> 
{
    /**
     * The metric to find on the complementary graph
     */
    private final VertexMetric<U> metric;
    
    /**
     * Constructor.
     * @param metric the metric to find on the complementary graph. 
     */
    public ComplementaryVertexMetric(VertexMetric<U> metric)
    {
        this.metric = metric;
    }
    
    @Override
    public double compute(Graph<U> graph, U user)
    {
        if(!graph.isMultigraph())
            return Double.NaN;
        return metric.compute(graph.complement(), user);
    }
    
}
