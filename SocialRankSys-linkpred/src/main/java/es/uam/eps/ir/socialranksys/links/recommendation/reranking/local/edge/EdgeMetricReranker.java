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
import es.uam.eps.ir.ranksys.core.util.Stats;
import es.uam.eps.ir.ranksys.novdiv.reranking.LambdaReranker;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.metrics.PairMetric;

/**
 * Reranks a graph according to a global graph metric which we want to improve.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public abstract class EdgeMetricReranker<U> extends LambdaReranker<U,U>
{
    /**
     * The graph.
     */
    protected final Graph<U> graph;
    
    /**
     * The selected metric
     */
    protected final PairMetric<U> metric;
    
    /**
     * Indicates if the scores have to be normalized.
     */
    protected final boolean norm;
    /**
     * Constructor.
     * @param lambda Param that establishes a balance between the score and the 
     * novelty/diversity value.
     * @param cutoff Number of elements to take.
     * @param norm Indicates if scores have to be normalized.
     * @param graph The graph.
     * @param graphMetric The graph metric to optimize.
     */
    public EdgeMetricReranker(double lambda, int cutoff, boolean norm, Graph<U> graph, PairMetric<U> graphMetric) 
    {
        super(lambda, cutoff, norm);
        this.graph = graph;
        this.metric = graphMetric;
        this.norm = norm;
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
         * @param recommendation the recommendation to be reranked.
         * @param maxLength the maximum length of the definitive ranking
         * @param graph the graph
         * @param metric the metric
         */
        public GraphMetricEdgeReranker(Recommendation<U, U> recommendation, int maxLength, Graph<U> graph, PairMetric<U> metric)
        {
            super(recommendation, maxLength);
            this.graph = graph;
            this.metric = metric;
            
        }

        @Override
        protected double norm(double score, Stats stats)
        {
            if(norm)
            {
                return (score - stats.getMin())/(stats.getMax()-stats.getMin());
            }
            else
                return score;
        }
        
        

        
        
    }
    
}
