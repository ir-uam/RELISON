/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.reranking.local.graph;

import com.rits.cloning.Cloner;
import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.links.recommendation.reranking.normalizer.Normalizer;
import es.uam.eps.ir.relison.metrics.GraphMetric;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.function.Supplier;

/**
 * Individual reranker, which reorders a recommendation to promote
 * a graph metric.
 *
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class DirectGraphMetricReranker<U> extends GraphMetricReranker<U> 
{
    /**
     * Constructor.
     * @param lambda        param that establishes a balance between the score and the novelty/diversity value.
     * @param cutoff        number of elements to take.
     * @param norm          the normalization function.
     * @param graph         the graph.
     * @param metric        the metric to optimize.
     */
    public DirectGraphMetricReranker(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, GraphMetric<U> metric)
    {
        super(lambda, cutoff, norm, graph, metric);
    }

    @Override
    protected GreedyUserReranker<U, U> getUserReranker(Recommendation<U, U> recommendation, int maxLength) {
        Cloner cloner = new Cloner();
        return new DirectGraphMetricUserReranker(recommendation, maxLength, cloner.deepClone(this.graph), this.metric);
    }

    /**
     * The individual reranker.
     */
    protected class DirectGraphMetricUserReranker extends GraphMetricUserReranker
    {
        /**
         * Constructor.
         * @param recommendation    the recommendation to rerank.
         * @param maxLength         the maximum length of the definitive ranking.
         * @param graph             the network.
         * @param metric            the metric to promote.
         */
        public DirectGraphMetricUserReranker(Recommendation<U, U> recommendation, int maxLength, Graph<U> graph, GraphMetric<U> metric) 
        {
            super(recommendation, maxLength, graph, metric);
        }

        @Override
        protected double nov(Tuple2od<U> iv) {
            U user = recommendation.getUser();
            U item = iv.v1;
            
            Cloner cloner = new Cloner();
            Graph<U> cloneGraph = cloner.deepClone(this.graph);
            cloneGraph.addEdge(user, item);
            return metric.compute(cloneGraph);
        }
        
    }
    
}
