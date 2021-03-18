/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.globalranking.edge;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.globalranking.GlobalRankingLambdaReranker;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.normalizer.Normalizer;
import es.uam.eps.ir.socialranksys.metrics.PairMetric;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.function.Supplier;

/**
 * Global reranker strategy that reorders the candidate users according to
 * an edge metric that we want to optimize.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class EdgeMetricReranker<U> extends GlobalRankingLambdaReranker<U,U>
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
     * @param lambda        trade-off between the recommendation score and the novelty/diversity value.
     * @param cutoff        number of elements to take.
     * @param norm          the normalization strategy.
     * @param graph         the original graph.
     * @param metric        the metric we want to optimize.
     */
    public EdgeMetricReranker(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, PairMetric<U> metric)
    {
        super(lambda, cutoff, norm);
        this.graph = graph;
        this.metric = metric;
    }

    @Override
    protected void update(U user, Tuple2od<U> bestItemValue)
    {
        U item = bestItemValue.v1;
        this.graph.addEdge(user, item);
    }    
}
