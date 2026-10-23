/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.sna.metrics;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.sna.community.Communities;

import java.util.Map;

/**
 * Computes a metric for each individual community.
 *
 * @param <U> Type of the nodes.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface IndividualEdgeAttributeMetric<U>
{
    /**
     * Computes the value of the metric for a single user.
     *
     * @param graph The graph.
     * @param attr_name The name of the node attribute
     * @param indiv Individual attribute
     *
     * @return the value of the metric.
     */
    double compute(Graph<U> graph, String attr_name, Object indiv);

    /**
     * Computes the value of the metric for all the users in the graph.
     *
     * @param graph The graph.
     * @param attr_name The name of the node attribute
     *
     * @return A map relating the attributes with the values of the metric.
     */
    Map<Object, Double> compute(Graph<U> graph, String attr_name);

    /**
     * Computes the average value of the metric in the graph.
     *
     * @param graph The graph.
     * @param attr_name The name of the node attribute
     *
     * @return the average value of the metric.
     */
    double averageValue(Graph<U> graph, String attr_name);
}
