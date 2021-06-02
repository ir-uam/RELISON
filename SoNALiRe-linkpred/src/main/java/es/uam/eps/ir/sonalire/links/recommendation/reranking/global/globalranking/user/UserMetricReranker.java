/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.reranking.global.globalranking.user;

import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.links.recommendation.reranking.global.globalranking.GlobalRankingLambdaReranker;
import es.uam.eps.ir.sonalire.links.recommendation.reranking.normalizer.Normalizer;
import es.uam.eps.ir.sonalire.metrics.VertexMetric;

import java.util.function.Supplier;

/**
 * Global reranker strategy that reorders the candidate users according to
 * a user metric that we want to optimize.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class UserMetricReranker<U> extends GlobalRankingLambdaReranker<U,U>
{
    /**
     * The original graph.
     */
    protected final Graph<U> graph;
    
    /**
     * The selected metric
     */
    protected final VertexMetric<U> metric;

    /**
     * Constructor.
     * @param lambda        trade-off between the recommendation score and the novelty/diversity value.
     * @param cutoff        number of elements to take.
     * @param norm          the normalization strategy.
     * @param graph         the original graph.
     * @param metric        the metric we want to optimize.
     */
    public UserMetricReranker(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, VertexMetric<U> metric)
    {
        super(lambda, cutoff, norm);
        this.graph = graph;
        this.metric = metric;
    }
}
