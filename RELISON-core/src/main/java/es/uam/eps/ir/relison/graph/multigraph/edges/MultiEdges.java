/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.graph.multigraph.edges;

import es.uam.eps.ir.relison.graph.edges.fast.FastEdge;
import es.uam.eps.ir.relison.graph.multigraph.edges.fast.FastMultiEdge;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Interface that represents the edges of a graph.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface MultiEdges
{
    /**
     * Checks if an edge exists.
     *
     * @param orig The origin endpoint
     * @param dest The destiny endpoint
     *
     * @return true if the edge exists, false if not.
     */
    boolean containsEdge(int orig, int dest);

    /**
     * Gets the number of existing edges between two destinies
     *
     * @param orig The origin endpoint
     * @param dest The destiny endpoint
     *
     * @return the number of edges
     */
    int getNumEdges(int orig, int dest);

    /**
     * Gets the weight of an edge.
     *
     * @param orig The origin endpoint.
     * @param dest The destiny endpoint.
     *
     * @return the value if the edge exists, the default error value if not.
     */
    List<Double> getEdgeWeights(int orig, int dest);

    /**
     * Gets the type of an edge.
     *
     * @param orig The origin endpoint.
     * @param dest The destiny endpoint.
     *
     * @return the type if the edge exists, the default error value if not.
     */
    List<Integer> getEdgeTypes(int orig, int dest);

    /**
     * Gets the incoming neighbourhood of a node.
     *
     * @param node The node.
     *
     * @return a stream of all the ids of nodes.
     */
    Stream<Integer> getIncidentNodes(int node);

    /**
     * Gets the outgoing neighbourhood of a node.
     *
     * @param node The node.
     *
     * @return a stream containing all the ids of the nodes.
     */
    Stream<Integer> getAdjacentNodes(int node);

    /**
     * Gets the full neighbourhood of a node.
     *
     * @param node The node.
     *
     * @return a stream containing all the ids of the nodes.
     */
    Stream<Integer> getNeighbourNodes(int node);

    /**
     * Gets the neighbors of the user with whom he has both incoming and outgoing links.
     * @param node The node.
     * @return a stream containing the ids of the mutual neighborhood.
     */
    Stream<Integer> getMutualNodes(int node);

    /**
     * Gets the types of the incident edges of a node.
     *
     * @param node The node.
     *
     * @return a stream containing all the edge types.
     */
    Stream<MultiEdgeTypes> getIncidentTypes(int node);

    /**
     * Gets the types of the adjacent edges of a node.
     *
     * @param node The node.
     *
     * @return a stream containing all the edge types.
     */
    Stream<MultiEdgeTypes> getAdjacentTypes(int node);

    /**
     * Gets the types of the neighbourhood edges of a node.
     *
     * @param node The node.
     *
     * @return a stream containing all the edge types.
     */
    Stream<MultiEdgeTypes> getNeighbourTypes(int node);

    /**
     * Gets the types of the neighbourhood edges of a node.
     *
     * @param node The node.
     *
     * @return a stream containing all the edge types.
     */
    Stream<MultiEdgeTypes> getMutualTypes(int node);

    /**
     * Gets the weights of the incident edges of a node.
     *
     * @param node The node.
     *
     * @return a stream containing all the edge types.
     */
    Stream<MultiEdgeWeights> getIncidentWeights(int node);

    /**
     * Gets the weights of the adjacent edges of a node.
     *
     * @param node The node.
     *
     * @return a stream containing all the edge types.
     */
    Stream<MultiEdgeWeights> getAdjacentWeights(int node);

    /**
     * Gets the weights of the neighbour edges of a node.
     *
     * @param node The node.
     *
     * @return a sream containing all the edge types.
     */
    Stream<MultiEdgeWeights> getNeighbourWeights(int node);

    /**
     * Gets the weights of the mutual edges of a node.
     *
     * @param node The node.
     *
     * @return a sream containing all the edge types.
     */
    Stream<MultiEdgeWeights> getMutualWeights(int node);

    /**
     * Gets the weights of the adjacent mutual edges of a node.
     *
     * @param node The node.
     *
     * @return a stream containing all the edge weights.
     */
    Stream<MultiEdgeWeights> getMutualAdjacentWeights(int node);

    /**
     * Gets the weights of the incident mutual edges of a node.
     *
     * @param node The node.
     *
     * @return a stream containing all the edge weights.
     */
    Stream<MultiEdgeWeights> getMutualIncidentWeights(int node);

    /**
     * Gets the types of the adjacent mutual edges of a node.
     *
     * @param node The node.
     *
     * @return a stream containing all the edge types.
     */
    Stream<MultiEdgeTypes> getMutualAdjacentTypes(int node);

    /**
     * Gets the types of the incident mutual edges of a node.
     *
     * @param node The node.
     *
     * @return a stream containing all the edge types.
     */
    Stream<MultiEdgeTypes> getMutualIncidentTypes(int node);

    /**
     * Adds a user to the edges.
     *
     * @param idx Identifier of the user
     *
     * @return the user.
     */
    boolean addUser(int idx);

    /**
     * Adds an edge to the set.
     *
     * @param orig   Origin node
     * @param dest   Destiny node
     * @param weight Weight of the edge
     * @param type   Type of the edge
     *
     * @return true if everything went OK, false if not.
     */
    boolean addEdge(int orig, int dest, double weight, int type);

    /**
     * Gets the number of incident edges to a node
     *
     * @param dest Destiny node
     *
     * @return the number of incident edges
     */
    int getIncidentCount(int dest);

    /**
     * Gets the number of adjacent edges to a node
     *
     * @param orig Origin node
     *
     * @return the number of adjacent edges
     */
    int getAdjacentCount(int orig);

    /**
     * Gets the number of edges which reach or start in a certain node
     *
     * @param node the node
     *
     * @return the number of edges which reach or start in that node
     */
    int getNeighbourCount(int node);

    /**
     * Obtains the number of edges
     *
     * @return the number of edges
     */
    long getNumEdges();

    /**
     * Obtains the set of nodes which do not have any neighbor.
     *
     * @return a stream containing the set of nodes which do not have neighbors.
     */
    IntStream getIsolatedNodes();

    /**
     * Obtains the set of nodes which have incident edges.
     *
     * @return a stream containing the ids of nodes with incident edges
     */
    IntStream getNodesWithIncidentEdges();

    /**
     * Obtains the set of nodes which have adjacent edges.
     *
     * @return a stream containing the ids of nodes with incident edges
     */
    IntStream getNodesWithAdjacentEdges();

    /**
     * Obtains the set of nodes which have edges.
     *
     * @return a stream containing the ids of nodes with incident edges
     */
    IntStream getNodesWithEdges();

    /**
     * Obtains the set of nodes which have, at least, a reciprocal edge.
     *
     * @return a stream containing the ids of nodes with reciprocal edges.
     */
    IntStream getNodesWithMutualEdges();

    /**
     * Checks if a node has adjacent edges.
     *
     * @param idx the identifier of the node
     *
     * @return true if it has adjacent edges, false otherwise
     */
    boolean hasAdjacentEdges(int idx);

    /**
     * Checks if a node has incident edges.
     *
     * @param idx the identifier of the node
     *
     * @return true if it has incident edges, false otherwise
     */
    boolean hasIncidentEdges(int idx);

    /**
     * Checks if a node has adjacent or incident edges.
     *
     * @param idx the identifier of the node
     *
     * @return true if it has adjacent or incident edges, false otherwise
     */
    boolean hasEdges(int idx);

    /**
     * Checks if a node has mutual edges.
     *
     * @param idx the identifier of the node
     *
     * @return true if it has mutual edges, false otherwise
     */
    boolean hasMutualEdges(int idx);

    /**
     * Removes an edge.
     *
     * @param orig  source node.
     * @param dest  incoming node.
     * @param idx   the number of the edge
     *
     * @return true if everything went OK, false if not.
     */
    boolean removeEdge(int orig, int dest, int idx);

    /**
     * Removes a node from the edge list.
     *
     * @param idx the identifier of the node.
     *
     * @return true if everything went OK, false otherwise.
     */
    boolean removeNode(int idx);

    /**
     * Removes all the edges between a pair of nodes.
     *
     * @param orig source node.
     * @param dest incoming node.
     *
     * @return true if everything went OK, false otherwise.
     */
    boolean removeEdges(int orig, int dest);

    /**
     * Modifies the weight of an edge.
     *
     * @param orig   source node.
     * @param dest   incoming node.
     * @param idx    the number of the edge.
     * @param weight the new weight of the edge.
     *
     * @return true if everything went OK, false if not, or the edge does not exist.
     */
    boolean updateEdgeWeight(int orig, int dest, double weight, int idx);

    /**
     * Modifies the weight of an edge.
     *
     * @param orig   source node.
     * @param dest   incoming node.
     * @param idx    the number of the edge.
     * @param type   the new type of the edge.
     *
     * @return true if everything went OK, false if not, or the edge does not exist.
     */
    boolean updateEdgeType(int orig, int dest, int type, int idx);

    Stream<FastMultiEdge> getAdjacentEdges(int idx);
    Stream<FastMultiEdge> getIncidentEdges(int idx);
    Stream<FastMultiEdge> getMutualEdges(int idx);
    Stream<FastMultiEdge> getNeighbourEdges(int idx);
    Stream<FastMultiEdge> getMutualAdjacentEdges(int idx);
    Stream<FastMultiEdge> getMutualIncidentEdges(int idx);
}
