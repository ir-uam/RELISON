/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.local.edge;

import com.rits.cloning.Cloner;
import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.normalizer.Normalizer;
import es.uam.eps.ir.socialranksys.metrics.PairMetric;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.function.Supplier;

/**
 * Global reranker strategy that optimizes the average value of an edge metric.
 * Metrics are updated as we add edges to the graph.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class ProgressiveDirectEdgeMetricReranker<U> extends EdgeMetricReranker<U> 
{
    /**
     * Constructor.
     * @param lambda        trade-off between the recommendation score and the novelty/diversity value.
     * @param cutoff        number of elements to take.
     * @param norm          the normalization strategy.
     * @param graph         the original graph.
     * @param metric        the metric we want to optimize.
     */
    public ProgressiveDirectEdgeMetricReranker(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, PairMetric<U> metric)
    {
        super(lambda, cutoff, norm, graph, metric);
    }


    @Override
    protected double nov(U u, Tuple2od<U> iv) {
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

        return metric.compute(cloneGraph, u, item);
    }

    @Override
    protected void innerUpdate(U user, Tuple2od<U> bestItemValue)
    {
    }

    @Override
    protected void update(Recommendation<U, U> reranked) {
    }    
}
