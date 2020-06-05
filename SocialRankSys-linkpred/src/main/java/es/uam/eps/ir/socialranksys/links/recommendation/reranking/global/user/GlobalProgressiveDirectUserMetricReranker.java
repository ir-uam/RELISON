/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.user;

import com.rits.cloning.Cloner;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.metrics.VertexMetric;
import org.ranksys.core.util.tuples.Tuple2od;

/**
 * Reranks a graph according to a global graph metric which we want to update.
 * The value of the metric (updated every time a new graph is added) is taken as the novelty score.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class GlobalProgressiveDirectUserMetricReranker<U> extends GlobalUserMetricReranker<U> 
{

    /**
     * Constructor
     * @param lambda Trade-off between original and novelty scores
     * @param cutoff Maximum length of the recommendation graph.
     * @param norm True if scores have to be normalized, false if not.
     * @param graph The original graph.
     * @param vertexMetric The vertex metric we want to compute.
     */
    public GlobalProgressiveDirectUserMetricReranker(double lambda, int cutoff, boolean norm, Graph<U> graph, VertexMetric<U> vertexMetric)
    {
        super(lambda, cutoff, norm, graph, vertexMetric);
    }

    @Override
    protected double nov(U user, Tuple2od<U> iv) {
        U item = iv.v1;

        Cloner cloner = new Cloner();
        Graph<U> cloneGraph = cloner.deepClone(graph);
        cloneGraph.addEdge(user, item);

        return metric.compute(cloneGraph, item);  
    }

    @Override
    protected void update(U user, Tuple2od<U> iv) {
        U item =iv.v1;
        this.graph.addEdge(user, item);
    }
    
}