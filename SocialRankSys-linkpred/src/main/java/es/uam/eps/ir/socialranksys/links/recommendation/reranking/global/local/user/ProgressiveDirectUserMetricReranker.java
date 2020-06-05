/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.local.user;

import com.rits.cloning.Cloner;
import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.metrics.VertexMetric;
import org.ranksys.core.util.tuples.Tuple2od;

/**
 * Reranks a graph according to an average vertex metric which we want to maximize.
 * The value of the metric is taken as the novelty score, and updated every time 
 * we add a new recommended user.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class ProgressiveDirectUserMetricReranker<U> extends UserMetricReranker<U> 
{
    /**
     * Constructor.
     * @param lambda Param that establishes a balance between the score and the 
     * novelty/diversity value.
     * @param cutoff Number of elements to take.
     * @param norm Indicates if scores have to be normalized.
     * @param graph The graph.
     * @param graphMetric The graph metric to maximize.
     * @param rank Indicates if the normalization is by ranking (true) or by score (false)
     */
    public ProgressiveDirectUserMetricReranker(double lambda, int cutoff, boolean norm, boolean rank, Graph<U> graph, VertexMetric<U> graphMetric)
    {
        super(lambda, cutoff, norm, rank, graph, graphMetric);
    }


    @Override
    protected double nov(U u, Tuple2od<U> iv) {
        U item = iv.v1;
            
        Cloner cloner = new Cloner();
        Graph<U> cloneGraph = cloner.deepClone(graph);
        cloneGraph.addEdge(u, item);

        return metric.compute(cloneGraph, item); 
    }

    @Override
    protected void update(U user, Tuple2od<U> bestItemValue)
    {
        U item = bestItemValue.v1;
        this.graph.addEdge(user, item);
    }

    @Override
    protected void update(Recommendation<U, U> reranked) {
    }    
}
