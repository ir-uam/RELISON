/*
 *  Copyright (C) 2024 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.seed.selector;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.sna.metrics.VertexMetric;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Selects the top-{@code k} (or bottom-{@code k}) users of the network according to a vertex metric — e.g. the 50
 * highest-degree or lowest-PageRank nodes. The metric is computed over the network the selector is applied to.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 *
 * @param <U> type of the users.
 */
public class MetricUserSelector<U> implements UserSelector<U>
{
    /**
     * Whether the highest-scoring ({@link #TOP}) or lowest-scoring ({@link #BOTTOM}) users are selected.
     */
    public enum Mode
    {
        /** The highest-scoring users. */
        TOP,
        /** The lowest-scoring users. */
        BOTTOM
    }

    /**
     * The vertex metric to rank users by.
     */
    private final VertexMetric<U> metric;
    /**
     * The number of users to select.
     */
    private final int k;
    /**
     * Whether to take the top or the bottom of the ranking.
     */
    private final Mode mode;

    /**
     * Constructor.
     * @param metric the vertex metric to rank the users by.
     * @param k      the number of users to select.
     * @param mode   whether to take the top ({@link Mode#TOP}) or bottom ({@link Mode#BOTTOM}) of the ranking.
     */
    public MetricUserSelector(VertexMetric<U> metric, int k, Mode mode)
    {
        this.metric = metric;
        this.k = Math.max(0, k);
        this.mode = mode;
    }

    @Override
    public Set<U> select(Graph<U> graph)
    {
        Map<U, Double> values = this.metric.compute(graph);
        Comparator<Map.Entry<U, Double>> byValue = Map.Entry.comparingByValue();
        if (this.mode == Mode.TOP) byValue = byValue.reversed();

        return values.entrySet().stream()
                .sorted(byValue)
                .limit(this.k)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
