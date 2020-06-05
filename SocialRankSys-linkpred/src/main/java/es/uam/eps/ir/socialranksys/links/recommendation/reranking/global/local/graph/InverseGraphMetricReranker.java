/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.local.graph;

import com.rits.cloning.Cloner;
import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.metrics.GraphMetric;
import org.ranksys.core.util.tuples.Tuple2od;

/**
 * Reranks a graph according to a global graph metric which we want to update.
 * The negative value of the metric is taken as the novelty score.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class InverseGraphMetricReranker<U> extends GraphMetricReranker<U> 
{
    /**
     * Constructor.
     * @param lambda Param that establishes a balance between the score and the 
     * novelty/diversity value.
     * @param cutoff Number of elements to take.
     * @param norm Indicates if scores have to be normalized.
     * @param graph The graph.
     * @param graphMetric the global graph metric to minimize
     * @param rank Indicates if the normalization is by ranking (true) or by score (false)
     */
    public InverseGraphMetricReranker(double lambda, int cutoff, boolean norm, boolean rank, Graph<U> graph, GraphMetric<U> graphMetric)
    {
        super(lambda, cutoff, norm, rank, graph, graphMetric);
    }

    @Override
    protected double nov(U u, Tuple2od<U> iv)
    {
        U item = iv.v1;
        
        Cloner cloner  = new Cloner();
        Graph<U> cloneGraph = cloner.deepClone(this.graph);
        cloneGraph.addEdge(u, item);
        return -metric.compute(cloneGraph);
    }

    @Override
    protected void update(Recommendation<U, U> reranked)
    {
    }
    
}
