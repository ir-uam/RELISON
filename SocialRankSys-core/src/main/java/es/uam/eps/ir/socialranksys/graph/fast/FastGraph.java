/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph.fast;

import es.uam.eps.ir.ranksys.fast.preference.IdxPref;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeType;
import es.uam.eps.ir.socialranksys.index.Index;
import es.uam.eps.ir.socialranksys.index.ReducedIndex;

import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Interface for fast implementations of graphs.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface FastGraph<U> extends Graph<U>, ReducedIndex<U>
{
    /**
     * Obtains the identifiers of all the nodes in the network.
     * @return an stream containing the vertex identifiers.
     */
    IntStream getAllNodesIds();
    /**
     * Obtains the identifiers of the nodes which have edges.
     * @param direction the orientation selection.
     * @return a stream containing the corresponding nodes.
     */
    IntStream getNodesIdsWithEdges(EdgeOrientation direction);

    /**
     * Obtains the set of isolated nodes in the graph.
     * @return a stream containing the isolated nodes.
     */
    IntStream getIsolatedNodeIds();
    /**
     * Checks whether the network contains an edge or not.
     * @param uidx the identifier of the first vertex
     * @param vidx the identifier of the second vertex
     * @return true if the edge exists, false otherwise.
     */
    boolean containsEdge(int uidx, int vidx);
    /**
     * Uncontrolled edge addition method, using ids.
     *
     * @param nodeA  Identifier of the first node.
     * @param nodeB  Identifier of the second node.
     * @param weight Weight of the link.
     * @param type   Type of the link.
     *
     * @return true if everything went ok, false otherwise.
     */
    boolean addEdge(int nodeA, int nodeB, double weight, int type);
    /**
     * Uncontrolled edge update method, using ids.
     *
     * @param nodeA  Identifier of the first node.
     * @param nodeB  Identifier of the second node.
     * @param weight Weight of the link.
     *
     * @return true if everything went ok, false otherwise.
     */
    boolean updateEdgeWeight(int nodeA, int nodeB, double weight);
    /**
     * Obtains the weight of an edge, given the identifiers of the involved nodes.
     * @param uidx identifier of the first user.
     * @param vidx identifier of the second user.
     * @return the weight if it exists, an error value otherwise.
     */
    double getEdgeWeight(int uidx, int vidx);
    /**
     * Obtains the neighborhood of a node, given its identifier.
     * @param uidx the identifier of the node.
     * @param orientation the orientation of the neighborhood to retrieve.
     * @return an stream containing the neighbors of the node.
     */
    Stream<Integer> getNeighborhood(int uidx, EdgeOrientation orientation);
    /**
     * Obtains the neighborhood of a node and the weight of the edges to each other, given its identifier.
     * @param uidx the identifier of the node.
     * @param orientation the orientation of the neighborhood to retrieve.
     * @return an stream containing the weights of neighbors of the node.
     */
    Stream<IdxPref> getNeighborhoodWeights(int uidx, EdgeOrientation orientation);

    /**
     * Obtains the neighborhood of a node and the type of the edges to each other, given its identifier.
     * @param uidx the identifier of the node.
     * @param orientation the orientation of the neighborhood to retrieve.
     * @return an stream containing the edge types of the neighbors of the node.
     */
    Stream<EdgeType> getNeighborhoodTypes(int uidx, EdgeOrientation orientation);
    /**
     * Obtains the index for the vertices.
     *
     * @return the index for the vertices.
     */
    Index<U> getIndex();

}
