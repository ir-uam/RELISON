/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.reranking.local.user;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.links.recommendation.reranking.normalizer.Normalizer;
import es.uam.eps.ir.relison.sna.metrics.VertexMetric;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Reranker that minimizes the average value of vertex metric.
 * The value of the metric in the original graph is taken as the novelty score.
 *
 * Individually reranks each recommendation.
 *
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class OriginalInverseUserMetricReranker<U> extends UserMetricReranker<U> 
{
    /**
     * Constructor.
     * @param lambda        trade-off between the original recommendation score and the metric we want to promote.
     * @param cutoff        the size of the recommendation ranking.
     * @param norm          the normalization approach.
     * @param graph         the graph we want to use.
     * @param metric        the vertex metric to demote.
     */
    public OriginalInverseUserMetricReranker(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, VertexMetric<U> metric)
    {
        super(lambda, cutoff, norm, graph, metric);
    }

    @Override
    protected GreedyUserReranker<U, U> getUserReranker(Recommendation<U, U> recommendation, int maxLength)
    {
        return new OriginalInverseUserMetricUserReranker(recommendation,maxLength,this.graph,this.metric);
    }

    /**
     * The individual user reranker.
     */
    protected class OriginalInverseUserMetricUserReranker extends UserMetricUserReranker
    {
        /**
         * The values of the metric for each node.
         */
        private final Map<U,Double> values;

        /**
         * Constructor.
         * @param recommendation    the recommendation.
         * @param maxLength         the maximum number of candidate users to consider.
         * @param graph             the graph.
         * @param metric            the metric to optimize.
         */
        public OriginalInverseUserMetricUserReranker(Recommendation<U, U> recommendation, int maxLength, Graph<U> graph, VertexMetric<U> metric)
        {
            super(recommendation, maxLength, graph, metric);
            this.values = new HashMap<>();
        }  

        @Override
        protected void update(Tuple2od<U> bestItemValue)
        {
        }

        @Override
        protected double nov(Tuple2od<U> iv)
        {
            U item = iv.v1;
            if(values.containsKey(item))
                return values.get(item);
            double value = -metric.compute(graph, item);
            values.put(item, value);
            return value;        
        }
    }
    
}
