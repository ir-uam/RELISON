/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.complementary;

import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.metrics.IndividualCommunityMetric;

import java.util.Map;

/**
 * Computes an individual community metric over the complementary graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public class ComplementaryIndividualCommunityMetric<U> implements IndividualCommunityMetric<U>
{
    /**
     * The metric to find on the complementary graph
     */
    private final IndividualCommunityMetric<U> metric;
    
    /**
     * Constructor.
     * @param metric the metric to find on the complementary graph. 
     */
    public ComplementaryIndividualCommunityMetric(IndividualCommunityMetric<U> metric)
    {
        this.metric = metric;
    }
    
    @Override
    public double compute(Graph<U> graph, Communities<U> comm, int indiv)
    {
        if(!graph.isMultigraph())
            return Double.NaN;
        return metric.compute(graph.complement(), comm, indiv);
    }

    @Override
    public Map<Integer, Double> compute(Graph<U> graph, Communities<U> comm) 
    {
        if(!graph.isMultigraph())
            return null;
        return metric.compute(graph.complement(), comm);    
    }

    @Override
    public double averageValue(Graph<U> graph, Communities<U> comm) 
    {
        if(!graph.isMultigraph())
            return Double.NaN;
        return metric.averageValue(graph.complement(), comm);       
    }
    
}
