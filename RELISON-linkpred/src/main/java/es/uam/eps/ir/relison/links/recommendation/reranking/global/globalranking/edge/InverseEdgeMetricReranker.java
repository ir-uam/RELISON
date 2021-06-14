/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.reranking.global.globalranking.edge;

import com.rits.cloning.Cloner;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.links.recommendation.reranking.normalizer.Normalizer;
import es.uam.eps.ir.relison.sna.metrics.PairMetric;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.function.Supplier;

/**
 * Global reranker strategy that minimizes the average value of an edge metric.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class InverseEdgeMetricReranker<U> extends EdgeMetricReranker<U>
{
    /**
     * Constructor.
     * @param lambda        trade-off between the recommendation score and the novelty/diversity value.
     * @param cutoff        number of elements to take.
     * @param norm          the normalization strategy.
     * @param graph         the original graph.
     * @param metric        the metric we want to optimize.
     */
    public InverseEdgeMetricReranker(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, PairMetric<U> metric)
    {
        super(lambda, cutoff, norm, graph, metric);
    }

    @Override
    protected double nov(U user, Tuple2od<U> iv)
    {
        U item = iv.v1;

        Cloner cloner = new Cloner();
        Graph<U> cloneGraph = cloner.deepClone(this.graph);
        cloneGraph.addEdge(user, item);
        return -metric.compute(cloneGraph,user,item);
    }       
    
}
