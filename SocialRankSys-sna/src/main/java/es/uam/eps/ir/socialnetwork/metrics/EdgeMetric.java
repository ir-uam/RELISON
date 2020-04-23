/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.metrics;

import es.uam.eps.ir.socialnetwork.metrics.exception.InexistentEdgeException;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Interface for edge based metrics.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public interface EdgeMetric<U>
{
    /**
     * Computes the value of the metric for a single edge
     * @param graph The full graph.
     * @param orig The origin node of the edge.
     * @param dest The destiny node of the edge.
     * @return The value of the metric for that edge.
     * @throws InexistentEdgeException if the edge does not exist.
     */
    double compute(Graph<U> graph, U orig, U dest) throws InexistentEdgeException;
    /**
     * Computes the value of the metric for all the edges in the graph.
     * @param graph The full graph.
     * @return A map containing the metrics for each edge.
     */
    Map<Pair<U>, Double> compute(Graph<U> graph);
    
    /**
     * Computes the value of the metric for a selection of edges in the graph.
     * @param graph The full graph.
     * @param edges A stream containing the selected edges.
     * @return A map containing the metrics for each edge in the stream that exists. In case the link
     * does not exist, NaN is returned as the value for the metric.
     */
    Map<Pair<U>, Double> compute(Graph<U> graph, Stream<Pair<U>> edges);
    
    
    /**
     * Computes the average value of the metric
     * @param graph The full graph.
     * @return The average value of the metric
     */
    double averageValue(Graph<U> graph);
    
    /**
     * Computes the average value of a certain group of edges.
     * @param graph the full graph.
     * @param edges A stream containing the selected edges.
     * @param edgeCount The number of edges in the stream.
     * @return The average value of the metric.
     */
    double averageValue(Graph<U> graph, Stream<Pair<U>> edges, int edgeCount);
}
