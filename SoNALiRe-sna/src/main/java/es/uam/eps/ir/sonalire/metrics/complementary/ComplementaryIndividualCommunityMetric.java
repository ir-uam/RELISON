/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.metrics.complementary;

import es.uam.eps.ir.sonalire.community.Communities;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.metrics.IndividualCommunityMetric;

import java.util.Map;

/**
 * Computes an individual community metric over the complementary graph.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class ComplementaryIndividualCommunityMetric<U> implements IndividualCommunityMetric<U>
{
    /**
     * The metric to find on the complementary graph.
     */
    private final IndividualCommunityMetric<U> metric;

    /**
     * Constructor.
     *
     * @param metric the metric to find on the complementary graph.
     */
    public ComplementaryIndividualCommunityMetric(IndividualCommunityMetric<U> metric)
    {
        this.metric = metric;
    }

    @Override
    public double compute(Graph<U> graph, Communities<U> comm, int indiv)
    {
        if (!graph.isMultigraph())
        {
            return Double.NaN;
        }
        return metric.compute(graph.complement(), comm, indiv);
    }

    @Override
    public Map<Integer, Double> compute(Graph<U> graph, Communities<U> comm)
    {
        if (!graph.isMultigraph())
        {
            return null;
        }
        return metric.compute(graph.complement(), comm);
    }

    @Override
    public double averageValue(Graph<U> graph, Communities<U> comm)
    {
        if (!graph.isMultigraph())
        {
            return Double.NaN;
        }
        return metric.averageValue(graph.complement(), comm);
    }

}
