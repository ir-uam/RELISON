/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.local.user;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.metrics.VertexMetric;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.HashMap;
import java.util.Map;

/**
 * Reranks a graph according to a global graph metric which we want to update.
 * The value of the metric is taken as the novelty score.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class OriginalInverseUserMetricReranker<U> extends UserMetricReranker<U> 
{
    public OriginalInverseUserMetricReranker(double lambda, int cutoff, boolean norm, Graph<U> graph, VertexMetric<U> graphMetric)
    {
        super(lambda, cutoff, norm, graph, graphMetric);
    }

    @Override
    protected GreedyUserReranker<U, U> getUserReranker(Recommendation<U, U> recommendation, int maxLength) {
        return new OriginalDirectUserMetricUserReranker(recommendation,maxLength,this.graph,this.metric);
    }
    
    protected class OriginalDirectUserMetricUserReranker extends UserMetricUserReranker
    {
        private final Map<U,Double> values;
        
        public OriginalDirectUserMetricUserReranker(Recommendation<U, U> recommendation, int maxLength, Graph<U> graph, VertexMetric<U> metric) 
        {
            super(recommendation, maxLength, graph, metric);
            this.values = new HashMap<>();
        }  

        @Override
        protected void update(Tuple2od<U> bestItemValue)
        {
            U user = recommendation.getUser();
            U item = bestItemValue.v1;
            
            this.graph.addEdge(user, item);
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
