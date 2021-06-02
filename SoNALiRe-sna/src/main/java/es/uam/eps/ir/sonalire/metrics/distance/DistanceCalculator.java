/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.metrics.distance;

import es.uam.eps.ir.sonalire.community.Communities;
import es.uam.eps.ir.sonalire.graph.Graph;

import java.util.Map;

/**
 * Interface that defines methods for computing distance-based metrics for a network and retrieving them.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface DistanceCalculator<U>
{
    /**
     * Computes the distances between nodes in a network.
     *
     * @param graph the graph.
     *
     * @return true if everything went OK, false otherwise.
     */
    boolean computeDistances(Graph<U> graph);

    /**
     * Returns the node betweenness for each node in the network.
     *
     * @return a map containing the node betweenness for each node.
     */
    Map<U, Double> getNodeBetweenness();

    /**
     * Gets the value of node betweenness for a single node.
     *
     * @param node the value for the node.
     *
     * @return the node betweenness for that node.
     */
    double getNodeBetweenness(U node);

    /**
     * Gets all the values of the edge betweenness.
     *
     * @return the edge betweenness value for each edge.
     */
    Map<U, Map<U, Double>> getEdgeBetweenness();

    /**
     * Returns the edge betweenness of all the adjacent edges to a given node.
     *
     * @param node The node.
     *
     * @return a map containing the values of edge betweenness for all the adjacent links to the given node.
     */
    Map<U, Double> getEdgeBetweenness(U node);

    /**
     * Returns the edge betweenness of a single edge.
     *
     * @param orig origin node of the edge.
     * @param dest destination node of the edge.
     *
     * @return the betweenness if the edge exists, -1.0 if not.
     */
    double getEdgeBetweenness(U orig, U dest);

    /**
     * Returns all the distances between different pairs.
     *
     * @return the distances between pairs.
     */
    Map<U, Map<U, Double>> getDistances();

    /**
     * Return the distances between a node and the rest of nodes in the network.
     *
     * @param node the node.
     *
     * @return a map containing all the distances from the node to the rest of the network.
     */
    Map<U, Double> getDistancesFrom(U node);

    /**
     * Returns the distance between the network and an specific node.
     *
     * @param node the node.
     *
     * @return a map containing all the distances from each vertex in the network to the node.
     */
    Map<U, Double> getDistancesTo(U node);

    /**
     * Returns the distance between two nodes.
     *
     * @param orig origin node.
     * @param dest destination node.
     *
     * @return the distance between both nodes. if there is a path between them, +Infinity if not.
     */
    double getDistances(U orig, U dest);

    /**
     * Returns the number of geodesic paths between different pairs.
     *
     * @return the distances between pairs.
     */
    Map<U, Map<U, Double>> getGeodesics();

    /**
     * Return the number of geodesic paths between a node and the rest of nodes in the network.
     *
     * @param node the node.
     *
     * @return a map containing the number of geodesic paths from the node to the rest of the network.
     */
    Map<U, Double> getGeodesics(U node);

    /**
     * Returns the number of geodesic paths between two nodes.
     *
     * @param orig origin node.
     * @param dest destination node.
     *
     * @return the number of geodesic paths between both nodes if there is a path between them, 0.0 if not.
     */
    double getGeodesics(U orig, U dest);

    /**
     * Obtains the strongly connected components of the graph.
     *
     * @return the strongly connected components of the graph.
     */
    Communities<U> getSCC();

    /**
     * Obtains the average shortest path length, averaged over all the finite distance paths.
     * @return the average shortest path length.
     */
    double getASL();

    /**
     * Obtains the number of infinite length pairs.
     * @return the number of infinite length pairs.
     */
    double getInfiniteDistances();
}
