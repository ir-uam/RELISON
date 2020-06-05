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

import java.util.HashMap;
import java.util.Map;

/**
 * Reranks a graph according to a global graph metric which we want to update.
 * The inverse value of the metric is taken as the novelty score.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class OriginalInverseEdgeMetricReranker<U> extends EdgeMetricReranker<U> 
{
    /**
     * Constructor
     * @param lambda trade-off between the original ranking and the reranked one
     * @param cutoff maximum length of the definitive ranking.
     * @param norm true if the original score and the metric score have to be reranked.
     * @param graph the graph
     * @param graphMetric the graph metric we want to update.
     */
    public OriginalInverseEdgeMetricReranker(double lambda, int cutoff, boolean norm, Graph<U> graph, PairMetric<U> graphMetric)
    {
        super(lambda, cutoff, norm, graph, graphMetric);
    }

    @Override
    protected GreedyUserReranker<U, U> getUserReranker(Recommendation<U, U> recommendation, int maxLength) {
        Cloner cloner = new Cloner();
        return new OriginalInverseGraphMetricEdgeReranker(recommendation, maxLength, cloner.deepClone(this.graph), this.metric);
    }
    
    /**
     * Class that reranks an individual recommendation using the inverse value of an edge metric.
     */
    protected class OriginalInverseGraphMetricEdgeReranker extends GraphMetricEdgeReranker
    {
        /**
         * The values for each user in the ranking
         */
        private final Map<U, Double> values;
        
        /**
         * Constructor.
         * @param recommendation the recommendation we want to rerank.
         * @param maxLength the maximum length of the definitive ranking.
         * @param graph the graph.
         * @param metric the metric we want to use for reranking.
         */
        public OriginalInverseGraphMetricEdgeReranker(Recommendation<U, U> recommendation, int maxLength, Graph<U> graph, PairMetric<U> metric) 
        {
            super(recommendation, maxLength, graph, metric);
            this.values = new HashMap<>();
        }

        @Override
        protected double nov(Tuple2od<U> iv) {
            U user = recommendation.getUser();
            U item = iv.v1;
            
            if(values.containsKey(user))
                return values.get(user);
            double value = -metric.compute(graph, user, item);
            values.put(item, value);
            return value;
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
