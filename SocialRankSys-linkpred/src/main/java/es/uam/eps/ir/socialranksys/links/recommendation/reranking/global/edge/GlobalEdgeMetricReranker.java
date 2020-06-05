/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.edge;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.GlobalLambdaReranker;
import es.uam.eps.ir.socialranksys.metrics.PairMetric;
import org.ranksys.core.util.tuples.Tuple2od;

/**
 * Reranks a graph according to a global graph metric which we want to improve.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public abstract class GlobalEdgeMetricReranker<U> extends GlobalLambdaReranker<U,U>
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
     * @param edgeMetric The graph metric to optimize.
     */
    public GlobalEdgeMetricReranker(double lambda, int cutoff, boolean norm, Graph<U> graph, PairMetric<U> edgeMetric) 
    {
        super(lambda, cutoff, norm);
        this.graph = graph;
        this.metric = edgeMetric;
    }

    @Override
    protected void update(U user, Tuple2od<U> bestItemValue)
    {
        U item = bestItemValue.v1;

        this.graph.addEdge(user, item);
    }    
}
