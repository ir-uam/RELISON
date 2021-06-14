/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.reranking.local.graph;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.links.recommendation.reranking.local.LambdaReranker;
import es.uam.eps.ir.relison.links.recommendation.reranking.normalizer.Normalizer;
import es.uam.eps.ir.relison.sna.metrics.GraphMetric;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.function.Supplier;

/**
 * Individual reranker, which reorders a recommendation according to
 * a graph metric.
 *
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class GraphMetricReranker<U> extends LambdaReranker<U,U>
{
    /**
     * The graph.
     */
    protected final Graph<U> graph;
    
    /**
     * The selected metric
     */
    protected final GraphMetric<U> metric;

    /**
     * Constructor.
     * @param lambda        param that establishes a balance between the score and the novelty/diversity value.
     * @param cutoff        number of elements to take.
     * @param norm          the normalization function.
     * @param graph         the graph.
     * @param metric        the metric to optimize.
     */
    public GraphMetricReranker(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, GraphMetric<U> metric)
    {
        super(lambda, cutoff, norm);
        this.graph = graph;
        this.metric = metric;
    }

    /**
     * The user reranker.
     */
    protected abstract class GraphMetricUserReranker extends LambdaReranker<U,U>.LambdaUserReranker
    {
        /**
         * The values.
         */
        protected final Graph<U> graph;

        /**
         * The graph metric
         */
        protected final GraphMetric<U> metric;
        /**
         * Constructor.
         * @param recommendation    the recommendation to rerank.
         * @param maxLength         the maximum length of the definitive ranking.
         * @param graph             the network.
         * @param metric            the metric to promote.
         */
        public GraphMetricUserReranker(Recommendation<U, U> recommendation, int maxLength, Graph<U> graph, GraphMetric<U> metric)
        {
            super(recommendation, maxLength);
            this.graph = graph;
            this.metric = metric;
            
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
