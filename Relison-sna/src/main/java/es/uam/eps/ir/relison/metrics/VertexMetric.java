/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.metrics;

import es.uam.eps.ir.relison.graph.Graph;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Interface for user related metrics of graphs.
 *
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface VertexMetric<U>
{
    /**
     * Computes the value of the metric for a single user.
     *
     * @param graph The graph.
     * @param user  The user to compute.
     *
     * @return the value of the metric.
     */
    double compute(Graph<U> graph, U user);

    /**
     * Computes the value of the metric for all the users in the graph.
     *
     * @param graph The graph.
     *
     * @return A map relating the users with the values of the metric.
     */
    default Map<U, Double> compute(Graph<U> graph)
    {
        Map<U, Double> res = new HashMap<>();
        graph.getAllNodes().forEach(u -> res.put(u, this.compute(graph, u)));
        return res;
    }

    /**
     * Computes the value of the metric for a subset of the users in the graph.
     *
     * @param graph the graph.
     * @param users the stream of users.
     *
     * @return a map relating users with the values of the metric. Nodes not in
     *         the graph will not be included in the map.
     */
    default Map<U, Double> compute(Graph<U> graph, Stream<U> users)
    {
        Map<U, Double> res = new ConcurrentHashMap<>();
        users.forEach(u ->
        {
            if (graph.containsVertex(u))
            {
                res.put(u, this.compute(graph, u));
            }
        });
        return res;
    }

    /**
     * Computes the average value of the metric in the graph.
     *
     * @param graph The graph.
     *
     * @return the average value of the metric.
     */
    default double averageValue(Graph<U> graph)
    {
        return averageValue(graph, graph.getAllNodes());
    }

    /**
     * Computes the average value of the metric for a set of users.
     *
     * @param graph The graph.
     * @param users A stream of users.
     *
     * @return the average value of the metric for those users.
     */
    default double averageValue(Graph<U> graph, Stream<U> users)
    {
        Stream<U> filteredUsers = users.filter(graph::containsVertex);
        OptionalDouble optional = filteredUsers.mapToDouble(u -> this.compute(graph, u)).average();
        return optional.isPresent() ? optional.getAsDouble() : 0.0;
    }
}
