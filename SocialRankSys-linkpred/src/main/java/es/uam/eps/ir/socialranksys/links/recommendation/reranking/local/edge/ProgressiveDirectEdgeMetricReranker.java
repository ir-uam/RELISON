/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.local.edge;

import com.rits.cloning.Cloner;
import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.metrics.PairMetric;
import org.ranksys.core.util.tuples.Tuple2od;

/**
 * Reranks a graph according to a global graph metric which we want to update.
 * The value of the metric is taken as the novelty score. Each time an edge is 
 * added, the metric is updated.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class ProgressiveDirectEdgeMetricReranker<U> extends EdgeMetricReranker<U> 
{
    /**
     * Constructor
     * @param lambda trade-off between the original ranking and the reranked one
     * @param cutoff maximum length of the definitive ranking.
     * @param norm true if the original score and the metric score have to be reranked.
     * @param graph the graph
     * @param graphMetric the graph metric we want to update.
     */
    public ProgressiveDirectEdgeMetricReranker(double lambda, int cutoff, boolean norm, Graph<U> graph, PairMetric<U> graphMetric)
    {
        super(lambda, cutoff, norm, graph, graphMetric);
    }

    @Override
    protected GreedyUserReranker<U, U> getUserReranker(Recommendation<U, U> recommendation, int maxLength) {
        Cloner cloner = new Cloner();
        return new ProgressiveDirectGraphMetricEdgeReranker(recommendation, maxLength, cloner.deepClone(this.graph), this.metric);
    }
    
    /**
     * Class that reranks an individual recommendation using the value of the edge metric in the extended graph.
     */
    protected class ProgressiveDirectGraphMetricEdgeReranker extends GraphMetricEdgeReranker
    {
        /**
         * Constructor.
         * @param recommendation the recommendation we want to rerank.
         * @param maxLength the maximum length of the ranking.
         * @param graph the graph.
         * @param metric the metric.
         */
        public ProgressiveDirectGraphMetricEdgeReranker(Recommendation<U, U> recommendation, int maxLength, Graph<U> graph, PairMetric<U> metric) 
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
            return metric.compute(cloneGraph,user,item);
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
