/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.local.graph;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.local.LocalLambdaReranker;
import es.uam.eps.ir.socialranksys.metrics.GraphMetric;
import org.ranksys.core.util.tuples.Tuple2od;

/**
 * Reranks a graph according to a global graph metric which we want to maximize.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public abstract class GraphMetricReranker<U> extends LocalLambdaReranker<U,U>
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
     * @param graphMetric The graph metric to maximize.
     * @param rank Indicates if the normalization is by ranking (true) or by score (false)
     */
    public GraphMetricReranker(double lambda, int cutoff, boolean norm, boolean rank, Graph<U> graph, GraphMetric<U> graphMetric) 
    {
        super(cutoff, lambda, norm, rank);
        this.graph = graph;
        this.metric = graphMetric;
        this.norm = norm;
    }
    
    
    @Override
    protected void update(U user, Tuple2od<U> bestItemValue)
    {
        U item = bestItemValue.v1;
        this.graph.addEdge(user, item);
    }
    
}
