/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.local.edge;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.local.LambdaReranker;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.normalizer.Normalizer;
import es.uam.eps.ir.socialranksys.metrics.PairMetric;

import java.util.function.Supplier;

/**
 * Abstract implementation of a reranking algorithm that modifies the ranking according
 * to the values of an edge metric.
 *
 * Individually reranks each recommendation.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> Type of the users
 */
public abstract class EdgeMetricReranker<U> extends LambdaReranker<U,U>
{
    /**
     * The graph.
     */
    protected final Graph<U> graph;

    /**
     * The selected metric.
     */
    protected final PairMetric<U> metric;

    /**
     * Constructor.
     * @param lambda        trade-off between the recommendation score and the novelty/diversity value.
     * @param cutoff        number of elements to take.
     * @param norm          the normalization strategy.
     * @param graph         the original graph.
     * @param metric        the metric to optimize.
     */
    public EdgeMetricReranker(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, PairMetric<U> metric)
    {
        super(lambda, cutoff, norm);
        this.graph = graph;
        this.metric = metric;
    }


    /**
     * Class that reranks an individual recommendation using edge metrics.
     */
    protected abstract class GraphMetricEdgeReranker extends LambdaUserReranker
    {
        /**
         * The graph
         */
        protected final Graph<U> graph;
        /**
         * The pair metric
         */
        protected final PairMetric<U> metric;
        /**
         * Constructor
         * @param recommendation    the recommendation to be reranked.
         * @param maxLength         the maximum length of the definitive ranking.
         * @param graph             the graph.
         * @param metric            the metric.
         */
        public GraphMetricEdgeReranker(Recommendation<U, U> recommendation, int maxLength, Graph<U> graph, PairMetric<U> metric)
        {
            super(recommendation, maxLength);
            this.graph = graph;
            this.metric = metric;
            
        }
    }
    
}
