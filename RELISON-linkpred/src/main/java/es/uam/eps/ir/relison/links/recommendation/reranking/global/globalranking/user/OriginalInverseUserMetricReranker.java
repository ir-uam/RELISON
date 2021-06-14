/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.reranking.global.globalranking.user;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.links.recommendation.reranking.normalizer.Normalizer;
import es.uam.eps.ir.relison.sna.metrics.VertexMetric;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.function.Supplier;

/**
 * Global reranker that minimizes the average value of vertex metric.
 * The value of the metric in the original graph is taken as the novelty score.
 *
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class OriginalInverseUserMetricReranker<U> extends UserMetricReranker<U>
{
    /**
     * Constructor.
     * @param lambda        trade-off between the recommendation score and the novelty/diversity value.
     * @param cutoff        number of elements to take.
     * @param norm          the normalization strategy.
     * @param graph         the original graph.
     * @param metric        the metric we want to optimize.
     */
    public OriginalInverseUserMetricReranker(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, VertexMetric<U> metric)
    {
        super(lambda, cutoff, norm, graph, metric);
    }

    @Override
    protected double nov(U user, Tuple2od<U> item)
    {
        U recomm = item.v1;
        return -metric.compute(graph, recomm);
    }

    @Override
    protected void update(U user, Tuple2od<U> selectedItem)
    {

    }
    
}
