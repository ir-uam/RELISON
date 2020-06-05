/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.local.user;

import com.rits.cloning.Cloner;
import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.metrics.VertexMetric;
import org.ranksys.core.util.tuples.Tuple2od;

/**
 * Reranks a graph according to a global graph metric which we want to update.
 * The value of the metric is taken as the novelty score.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class ProgressiveDirectUserMetricReranker<U> extends UserMetricReranker<U> 
{

    public ProgressiveDirectUserMetricReranker(double lambda, int cutoff, boolean norm, Graph<U> graph, VertexMetric<U> graphMetric)
    {
        super(lambda, cutoff, norm, graph, graphMetric);
    }

    @Override
    protected GreedyUserReranker<U, U> getUserReranker(Recommendation<U, U> recommendation, int maxLength)
    {
        Cloner cloner = new Cloner();
        return new ProgressiveDirectUserMetricUserReranker(recommendation, maxLength, cloner.deepClone(graph), metric);
    }
    
    protected class ProgressiveDirectUserMetricUserReranker extends UserMetricUserReranker
    {
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
