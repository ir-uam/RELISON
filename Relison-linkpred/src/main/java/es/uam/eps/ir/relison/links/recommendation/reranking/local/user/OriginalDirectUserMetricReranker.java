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
 * Reranker that optimizes the average value of vertex metric.
 * The value of the metric in the original graph is taken as the novelty score.
 *
 * Individually reranks each recommendation.
 *
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class OriginalDirectUserMetricReranker<U> extends UserMetricReranker<U>
{
    /**
     * Constructor.
     * @param lambda        trade-off between the original recommendation score and the metric we want to promote.
     * @param cutoff        the size of the recommendation ranking.
     * @param norm          the normalization approach.
     * @param graph         the graph we want to use.
     * @param metric        the vertex metric to promote.
     */
    public OriginalDirectUserMetricReranker(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, VertexMetric<U> metric)
    {
        super(lambda, cutoff, norm, graph, metric);
    }

    @Override
    protected GreedyUserReranker<U, U> getUserReranker(Recommendation<U, U> recommendation, int maxLength)
    {
        return new OriginalDirectUserMetricUserReranker(recommendation,maxLength,this.graph,this.metric);
    }

    /**
     * The user reranker.
     */
    protected class OriginalDirectUserMetricUserReranker extends UserMetricUserReranker
    {
        /**
         * The values.
         */
        private final Map<U,Double> values;

        /**
         * Constructor.
         * @param recommendation    the recommendation to rerank.
         * @param maxLength         the maximum length of the definitive ranking.
         * @param graph             the network.
         * @param metric            the metric to promote.
         */
        public OriginalDirectUserMetricUserReranker(Recommendation<U, U> recommendation, int maxLength, Graph<U> graph, VertexMetric<U> metric) 
        {
            super(recommendation, maxLength, graph, metric);
            values = new HashMap<>();
        }  

        @Override
        protected void update(Tuple2od<U> bestItemValue)
        {
            // As we use the original value, it is not necessary to update anything.
        }

        @Override
        protected double nov(Tuple2od<U> iv)
        {
            U item = iv.v1;
            if(values.containsKey(item))
                return values.get(item);
            double value = metric.compute(graph, item);
            values.put(item, value);
            return value;
        }
    }
    
}
