/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.reranking.global.local.user;

import com.rits.cloning.Cloner;
import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.links.recommendation.reranking.normalizer.Normalizer;
import es.uam.eps.ir.sonalire.metrics.VertexMetric;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.function.Supplier;

/**
 * Implementation of a reranking strategy for contact recommendation that promotes the
 * average value of some vertex metric in the resulting network.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 */
public class ProgressiveDirectUserMetricReranker<U> extends UserMetricReranker<U> 
{
    /**
     * Constructor.
     * @param lambda    trade-off between the original and novelty scores
     * @param cutoff    maximum length of the definitive ranking.
     * @param norm      the normalization strategy.
     * @param graph     the original graph.
     * @param metric    the vertex metric to optimize.
     */
    public ProgressiveDirectUserMetricReranker(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, VertexMetric<U> metric)
    {
        super(lambda, cutoff, norm, graph, metric);
    }

    @Override
    protected double nov(U u, Tuple2od<U> iv)
    {
        U item = iv.v1;
        Graph<U> cloneGraph;
        if(graph.isDirected() || !graph.containsEdge(u, item))
        {
            Cloner cloner = new Cloner();
            cloneGraph = cloner.deepClone(graph);
            cloneGraph.addEdge(u, item);
        }
        else
            cloneGraph = graph;

        return metric.compute(cloneGraph, item);
    }

    @Override
    protected void innerUpdate(U user, Tuple2od<U> updated)
    {

    }

    @Override
    protected void update(Recommendation<U, U> reranked) {
    }
}
