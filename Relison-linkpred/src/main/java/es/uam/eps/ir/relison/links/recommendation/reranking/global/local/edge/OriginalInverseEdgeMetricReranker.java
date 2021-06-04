/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.reranking.global.local.edge;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.links.recommendation.reranking.normalizer.Normalizer;
import es.uam.eps.ir.relison.metrics.PairMetric;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Reranker strategy that minimizes the average value of an edge metric.
 * It uses the value of the metric for the original network as the novelty value.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class OriginalInverseEdgeMetricReranker<U> extends EdgeMetricReranker<U>
{
    /**
     * A map containing the edge metric values for each pair in the original graph.
     */
    private final Map<U, Map<U, Double>> values;

    /**
     * Constructor.
     * @param lambda        trade-off between the recommendation score and the novelty/diversity value.
     * @param cutoff        number of elements to take.
     * @param norm          the normalization strategy.
     * @param graph         the original graph.
     * @param metric        the metric we want to optimize.
     */
    public OriginalInverseEdgeMetricReranker(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, PairMetric<U> metric)
    {
        super(lambda, cutoff, norm, graph, metric);
        values = new HashMap<>();
    }


    @Override
    protected double nov(U u, Tuple2od<U> iv)
    {
        U item = iv.v1;

        if(values.containsKey(u))
        {
            if(values.get(u).containsKey(item))
                return values.get(u).get(item);
            double value = metric.compute(graph, u, item);
            values.get(u).put(item, value);
            return value;
        }

        double value = -metric.compute(graph, u, item);
        values.put(u, new HashMap<>());
        values.get(u).put(item, value);
        return value;
    }

    @Override
    protected void update(U user, Tuple2od<U> bestItemValue) {
    }

    @Override
    protected void update(Recommendation<U, U> reranked) {
    }

    @Override
    protected void innerUpdate(U user, Tuple2od<U> bestItemValue)
    {

    }




}
