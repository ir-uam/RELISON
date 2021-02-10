/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.complementary;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.metrics.EdgeMetric;
import es.uam.eps.ir.socialranksys.metrics.exception.InexistentEdgeException;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Computes a edge metric over the complementary graph. It should be noted that
 * InexistentEdgeException only appears if the edge does not exist in the complementary
 * graph, i.e. the edge does exist in the original graph.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class ComplementaryEdgeMetric<U> implements EdgeMetric<U>
{
    /**
     * The metric to find on the complementary graph.
     */
    private final EdgeMetric<U> metric;

    /**
     * Constructor.
     *
     * @param metric the metric to find on the complementary graph.
     */
    public ComplementaryEdgeMetric(EdgeMetric<U> metric)
    {
        this.metric = metric;
    }

    @Override
    public double compute(Graph<U> graph, U orig, U dest) throws InexistentEdgeException
    {
        if (!graph.isMultigraph())
        {
            return Double.NaN;
        }
        return metric.compute(graph.complement(), orig, dest);
    }

    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph)
    {
        if (!graph.isMultigraph())
        {
            return null;
        }
        return metric.compute(graph.complement());
    }

    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph, Stream<Pair<U>> edges)
    {
        if (!graph.isMultigraph())
        {
            return null;
        }
        return metric.compute(graph.complement(), edges);
    }

    @Override
    public double averageValue(Graph<U> graph)
    {
        if (!graph.isMultigraph())
        {
            return Double.NaN;
        }
        return metric.averageValue(graph.complement());
    }

    @Override
    public double averageValue(Graph<U> graph, Stream<Pair<U>> edges, int edgeCount)
    {
        if (!graph.isMultigraph())
        {
            return Double.NaN;
        }
        return metric.averageValue(graph.complement(), edges, edgeCount);
    }

}
