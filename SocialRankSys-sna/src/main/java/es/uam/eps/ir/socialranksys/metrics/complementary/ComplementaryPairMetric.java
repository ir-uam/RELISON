/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.complementary;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.metrics.PairMetric;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Computes a pair metric over the complementary graph.
 * graph, i.e. the edge does exist in the original graph.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class ComplementaryPairMetric<U> implements PairMetric<U>
{
    /**
     * The metric to find on the complementary graph
     */
    private final PairMetric<U> metric;

    /**
     * Constructor.
     *
     * @param metric the metric to find on the complementary graph.
     */
    public ComplementaryPairMetric(PairMetric<U> metric)
    {
        this.metric = metric;
    }

    @Override
    public double compute(Graph<U> graph, U orig, U dest)
    {
        if (graph.isMultigraph())
        {
            return Double.NaN;
        }
        return metric.compute(graph.complement(), orig, dest);
    }

    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph)
    {
        if (graph.isMultigraph())
        {
            return null;
        }
        return metric.compute(graph.complement());
    }

    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph, Stream<Pair<U>> pairs)
    {
        if (graph.isMultigraph())
        {
            return null;
        }
        return metric.compute(graph.complement(), pairs);
    }

    @Override
    public double averageValue(Graph<U> graph)
    {
        if (graph.isMultigraph())
        {
            return Double.NaN;
        }
        return metric.averageValue(graph.complement());
    }

    @Override
    public double averageValue(Graph<U> graph, Stream<Pair<U>> pairs, int edgeCount)
    {
        if (graph.isMultigraph())
        {
            return Double.NaN;
        }
        return metric.averageValue(graph.complement(), pairs, edgeCount);
    }

    @Override
    public Function<U, Double> computeOrig(Graph<U> graph, U orig)
    {
        if (graph.isMultigraph())
        {
            return v -> Double.NaN;
        }
        return metric.computeOrig(graph.complement(), orig);
    }

    @Override
    public Function<U, Double> computeDest(Graph<U> graph, U dest)
    {
        if (graph.isMultigraph())
        {
            return v -> Double.NaN;
        }
        return metric.computeDest(graph.complement(), dest);
    }

}
