/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.metrics;

import es.uam.eps.ir.sonalire.community.Communities;
import es.uam.eps.ir.sonalire.graph.Graph;

import java.util.Map;

/**
 * Computes a metric for each individual community.
 *
 * @param <U> Type of the nodes.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface IndividualCommunityMetric<U>
{
    /**
     * Computes the value of the metric for a single user.
     *
     * @param graph The graph.
     * @param comm  The relation between communities and nodes.
     * @param indiv Individual community
     *
     * @return the value of the metric.
     */
    double compute(Graph<U> graph, Communities<U> comm, int indiv);

    /**
     * Computes the value of the metric for all the users in the graph.
     *
     * @param graph The graph.
     * @param comm  The communities of the graph.
     *
     * @return A map relating the users with the values of the metric.
     */
    Map<Integer, Double> compute(Graph<U> graph, Communities<U> comm);

    /**
     * Computes the average value of the metric in the graph.
     *
     * @param graph The graph.
     * @param comm  the communities of the graph.
     *
     * @return the average value of the metric.
     */
    double averageValue(Graph<U> graph, Communities<U> comm);
}
