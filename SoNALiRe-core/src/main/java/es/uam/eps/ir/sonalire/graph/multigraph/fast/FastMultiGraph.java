/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.graph.multigraph.fast;

import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;
import es.uam.eps.ir.sonalire.graph.fast.FastGraph;
import es.uam.eps.ir.sonalire.graph.multigraph.MultiGraph;
import es.uam.eps.ir.sonalire.graph.multigraph.edges.MultiEdgeTypes;
import es.uam.eps.ir.sonalire.graph.multigraph.edges.MultiEdgeWeights;

import java.util.List;
import java.util.stream.Stream;

/**
 * Interface for fast implementations of multi-graphs.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface FastMultiGraph<U> extends FastGraph<U>, MultiGraph<U>
{
    /**
     * Gets the different weights for the edges of the selected neighbour nodes of a node.
     *
     * @param uidx        The identifier of the node to study
     * @param orientation The orientation to take
     *
     * @return A stream containing the weights
     */
    Stream<MultiEdgeWeights> getNeighbourhoodWeightsLists(int uidx, EdgeOrientation orientation);
    /**
     * Gets the different types for the edges of the selected neighbour nodes of a node.
     *
     * @param uidx        The identifier of the node to study
     * @param orientation The orientation to take
     *
     * @return A stream containing the types
     */
    Stream<MultiEdgeTypes> getNeighbourhoodTypesLists(int uidx, EdgeOrientation orientation);

    /**
     * Obtains the number of edges between a pair of vertices.
     * @param uidx the identifier of the source node.
     * @param vidx the identifier of the destination node.
     * @return the number of edges between the pair of nodes.
     */
    int getNumEdges(int uidx, int vidx);

    /**
     * Obtains the list of weights of the edges between a pair of vertices.
     * @param uidx the identifier of the source node.
     * @param vidx the identifier of the destination node.
     * @return the list of weights of the edges between the pair of nodes.
     */
    List<Double> getEdgeWeights(int uidx, int vidx);

    /**
     * Obtains the list of types of the edges between a pair of vertices.
     * @param uidx the identifier of the source node.
     * @param vidx the identifier of the destination node.
     * @return the list of types of the edges between the pair of nodes.
     */
    List<Integer> getEdgeTypes(int uidx, int vidx);
    /**
     * Updates the weight of an edge from the graph.
     *
     * @param orig The incident node of the edge to remove.
     * @param dest The adjacent node of the edge to remove.
     * @param weight the new weight for the edge.
     * @param idx The number of the edge to remove.
     *
     * @return true if everything went ok, false if not.
     */
    boolean updateEdgeWeight(U orig, U dest, double weight, int idx);

}
