/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.reranking.local.edge;

import com.rits.cloning.Cloner;
import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.links.recommendation.reranking.normalizer.Normalizer;
import es.uam.eps.ir.sonalire.metrics.PairMetric;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.function.Supplier;

/**
 * Reranker that minimizes the average value of an edge metric.
 * The value of the metric is updated each time we choose an edge to recommend.
 *
 * Individually reranks each recommendation.
 *
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class ProgressiveInverseEdgeMetricReranker<U> extends EdgeMetricReranker<U> 
{

    /**
     * Constructor.
     * @param lambda        trade-off between the recommendation score and the novelty/diversity value.
     * @param cutoff        number of elements to take.
     * @param norm          the normalization strategy.
     * @param graph         the original graph.
     * @param metric        the metric to optimize.
     */
    public ProgressiveInverseEdgeMetricReranker(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, PairMetric<U> metric)
    {
        super(lambda, cutoff, norm, graph, metric);
    }

    @Override
    protected GreedyUserReranker<U, U> getUserReranker(Recommendation<U, U> recommendation, int maxLength)
    {
        Cloner cloner = new Cloner();
        return new ProgressiveInverseGraphMetricEdgeReranker(recommendation, maxLength, cloner.deepClone(this.graph), this.metric);
    }
    
    /**
     * Class that reranks an individual recommendation using the edge metric value in the extended graph (adding all the previous edges).
     */
    protected class ProgressiveInverseGraphMetricEdgeReranker extends GraphMetricEdgeReranker
    {
        /**
         * Constructor.
         * @param recommendation    the recommendation we want to rerank.
         * @param maxLength         the maximum length of the ranking.
         * @param graph             the graph.
         * @param metric            the metric.
         */
        public ProgressiveInverseGraphMetricEdgeReranker(Recommendation<U, U> recommendation, int maxLength, Graph<U> graph, PairMetric<U> metric) 
        {
            super(recommendation, maxLength, graph, metric);
        }

        @Override
        protected double nov(Tuple2od<U> iv)
        {
            U user = recommendation.getUser();
            U item = iv.v1;
            
            Cloner cloner = new Cloner();
            Graph<U> cloneGraph = cloner.deepClone(this.graph);
            cloneGraph.addEdge(user, item);
            return -metric.compute(cloneGraph,user,item);
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
