/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.reranking.local.user;

import com.rits.cloning.Cloner;
import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.links.recommendation.reranking.normalizer.Normalizer;
import es.uam.eps.ir.sonalire.metrics.VertexMetric;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.function.Supplier;

/**
 * Implementation of a reranking strategy for contact recommendation that promotes the
 * average value of some vertex metric in the resulting network.
 *
 * Individually reranks each recommendation.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 */
public class ProgressiveDirectUserMetricReranker<U> extends UserMetricReranker<U> 
{
    /**
     * Constructor.
     * @param lambda        trade-off between the original recommendation score and the metric we want to promote.
     * @param cutoff        the size of the recommendation ranking.
     * @param norm          the normalization approach.
     * @param graph         the graph we want to use.
     * @param metric        the vertex metric to demote.
     */
    public ProgressiveDirectUserMetricReranker(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, VertexMetric<U> metric)
    {
        super(lambda, cutoff, norm, graph, metric);
    }

    @Override
    protected GreedyUserReranker<U, U> getUserReranker(Recommendation<U, U> recommendation, int maxLength)
    {
        Cloner cloner = new Cloner();
        return new ProgressiveDirectUserMetricUserReranker(recommendation, maxLength, cloner.deepClone(graph), metric);
    }

    /**
     * The class for generating the reranking.
     */
    protected class ProgressiveDirectUserMetricUserReranker extends UserMetricUserReranker
    {
        /**
         * Constructor.
         * @param recommendation    the recommendation to rerank.
         * @param maxLength         the maximum length of the definitive ranking.
         * @param graph             the network.
         * @param metric            the metric to promote.
         */
        public ProgressiveDirectUserMetricUserReranker(Recommendation<U, U> recommendation, int maxLength, Graph<U> graph, VertexMetric<U> metric) 
        {
            super(recommendation, maxLength, graph, metric);
        }        

        @Override
        protected double nov(Tuple2od<U> iv)
        {
            U user = recommendation.getUser();
            U item = iv.v1;
            
            Cloner cloner = new Cloner();
            Graph<U> cloneGraph = cloner.deepClone(graph);
            cloneGraph.addEdge(user, item);

            return metric.compute(cloneGraph, item);        
        }
        
        @Override
        protected void update(Tuple2od<U> bestItemValue)
        {
            U user = recommendation.getUser();
            U item = bestItemValue.v1;
            
            this.graph.addEdge(user, item);
        }
    }
    
}
