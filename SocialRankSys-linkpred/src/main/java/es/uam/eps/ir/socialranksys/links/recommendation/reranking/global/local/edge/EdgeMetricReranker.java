/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.local.edge;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.local.LocalLambdaReranker;
import es.uam.eps.ir.socialranksys.metrics.PairMetric;

/**
 * Reranks a graph according to an edge graph metric which we want to improve in average.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public abstract class EdgeMetricReranker<U> extends LocalLambdaReranker<U,U>
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
     * Constructor.
     * @param lambda Param that establishes a balance between the score and the 
     * novelty/diversity value.
     * @param cutoff Number of elements to take.
     * @param norm Indicates if scores have to be normalized.
     * @param graph The graph.
     * @param graphMetric The graph metric to optimize.
     * @param rank Indicates if the normalization is by ranking (true) or by score (false)
     */
    public EdgeMetricReranker(double lambda, int cutoff, boolean norm, boolean rank, Graph<U> graph, PairMetric<U> graphMetric) 
    {
        super(cutoff, lambda, norm, rank);
        this.graph = graph;
        this.metric = graphMetric;
    }    
}
