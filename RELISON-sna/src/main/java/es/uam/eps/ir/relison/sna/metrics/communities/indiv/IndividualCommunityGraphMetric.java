/*
 *  Copyright (C) 2026 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.sna.metrics.communities.indiv;

import es.uam.eps.ir.relison.utils.ComputationMonitor;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.generator.SubGraphGenerator;
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.sna.metrics.GraphMetric;
import es.uam.eps.ir.relison.sna.metrics.IndividualCommunityMetric;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Applies a graph metric independently to each community's induced subgraph.
 * Edges crossing community boundaries are excluded; nodes and edges inside
 * each community retain the source graph's direction and weights.
 *
 * @param <U> type of the graph nodes.
 */
public class IndividualCommunityGraphMetric<U> implements IndividualCommunityMetric<U>
{
    private final Supplier<GraphMetric<U>> metricSupplier;

    /**
     * Creates an individual community metric from a graph-metric supplier.
     * The supplier is called once per community so stateful metrics, such as
     * distance metrics with a calculator, are isolated between subgraphs.
     *
     * @param metricSupplier supplier for the graph metric to compute.
     */
    public IndividualCommunityGraphMetric(Supplier<GraphMetric<U>> metricSupplier)
    {
        this.metricSupplier = metricSupplier;
    }

    @Override
    public double compute(Graph<U> graph, Communities<U> communities, int community)
    {
        SubGraphGenerator<U> generator = new SubGraphGenerator<>();
        generator.configure(graph, communities.getUsers(community).collect(Collectors.toList()));
        try
        {
            return metricSupplier.get().compute(generator.generate());
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Could not create the induced subgraph for community " + community, e);
        }
    }

    @Override
    public Map<Integer, Double> compute(Graph<U> graph, Communities<U> communities)
    {
        Map<Integer, Double> values = ComputationMonitor.current().track(new LinkedHashMap<>(), "Computing community values", communities.getNumCommunities(), "communities");
        communities.getCommunities().forEach(community -> values.put(community, compute(graph, communities, community)));
        return values;
    }

    @Override
    public double averageValue(Graph<U> graph, Communities<U> communities)
    {
        return compute(graph, communities).values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
}
