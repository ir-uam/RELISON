/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.local.user;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.normalizer.Normalizer;
import es.uam.eps.ir.socialranksys.metrics.VertexMetric;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Reranker that optimizes the average value of vertex metric.
 * The value of the metric in the original graph is taken as the novelty score.
 *
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class OriginalDirectUserMetricReranker<U> extends UserMetricReranker<U> 
{
    /**
     * Metric values for each node in the graph
     */
    private final Map<U,Double> values;

    /**
     * Constructor.
     * @param lambda    trade-off between the original and novelty scores
     * @param cutoff    maximum length of the definitive ranking.
     * @param norm      the normalization strategy.
     * @param graph     the original graph.
     * @param metric    the vertex metric to optimize.
     */
    public OriginalDirectUserMetricReranker(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, VertexMetric<U> metric)
    {
        super(lambda, cutoff, norm, graph, metric);
        values = new HashMap<>();
    }


    @Override
    protected double nov(U u, Tuple2od<U> iv)
    {
        U item = iv.v1;
        if(values.containsKey(item))
            return values.get(item);
        double value = metric.compute(graph, item);
        values.put(item, value);
        return value;
    }

    @Override
    protected void innerUpdate(U user, Tuple2od<U> bestItemValue)
    {
    }

    @Override
    protected void update(U user, Tuple2od<U> bestItemValue)
    {

    }

    @Override
    protected void update(Recommendation<U, U> reranked)
    {
    }
    
}
