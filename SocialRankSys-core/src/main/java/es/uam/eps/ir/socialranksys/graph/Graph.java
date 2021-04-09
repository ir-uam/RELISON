/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph;

import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeType;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeWeight;
import es.uam.eps.ir.socialranksys.index.Index;

import java.io.Serializable;
import java.util.stream.Stream;

/**
 * Interface for a generic graph.
 *
 * @param <V> Type of vertices.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface Graph<V> extends Serializable
{
    /**
     * Adds a new node to the graph.
     *
     * @param node The new node to add.
     *
     * @return true if the node is correctly added, false if not.
     */
    boolean addNode(V node);

    /**
     * Adds a new edge to the graph. If nodes do not exist, they are added.
     *
     * @param nodeA The incident node.
     * @param nodeB The adjacent node.
     *
     * @return true if the edge is correctly added, false if not.
     */
    default boolean addEdge(V nodeA, V nodeB)
    {
        return addEdge(nodeA, nodeB, true);
    }

    /**
     * Adds an edge to the graph.
     *
     * @param nodeA       The incident node.
     * @param nodeB       The adjacent node.
     * @param insertNodes If true, nodes will be inserted if they do not exist. If false, the edge will only be added if both nodes are inserted.
     *
     * @return true if the edge is correctly added, false if not
     */
    default boolean addEdge(V nodeA, V nodeB, boolean insertNodes)
    {
        return addEdge(nodeA, nodeB, EdgeWeight.getDefaultValue(), EdgeType.getDefaultValue(), insertNodes);
    }

    /**
     * Adds a new edge to the graph. If nodes do not exist, they are added.
     *
     * @param nodeA  The incident node.
     * @param nodeB  The adjacent node.
     * @param weight The weight of the edge.
     *
     * @return true if the edge is correctly added, false if not.
     */
    default boolean addEdge(V nodeA, V nodeB, double weight)
    {
        return addEdge(nodeA, nodeB, weight, EdgeType.getDefaultValue(), true);
    }

    /**
     * Adds a new edge to the graph. If nodes do not exist, they are added.
     *
     * @param nodeA The incident node.
     * @param nodeB The adjacent node.
     * @param type  The edge type.
     *
     * @return true if the edge is correctly added, false if not.
     */
    default boolean addEdge(V nodeA, V nodeB, int type)
    {
        return addEdge(nodeA, nodeB, EdgeWeight.getDefaultValue(), type, true);
    }

    /**
     * Adds a new edge to the graph. If nodes do not exist, they are added.
     *
     * @param nodeA  The incident node.
     * @param nodeB  The adjacent node.
     * @param weight The weight of the edge.
     * @param type   The edge type.
     *
     * @return true if the edge is correctly added, false if not.
     */
    default boolean addEdge(V nodeA, V nodeB, double weight, int type)
    {
        return addEdge(nodeA, nodeB, weight, type, true);
    }

    /**
     * Adds a weighted edge to the graph.
     *
     * @param nodeA       The incident node.
     * @param nodeB       The adjacent node.
     * @param weight      The weight.
     * @param type        The edge type.
     * @param insertNodes If true, nodes will be inserted if they do not exist. If false, the edge will only be added if both nodes are inserted.
     *
     * @return if the edge is correctly added, false if not.
     */
    boolean addEdge(V nodeA, V nodeB, double weight, int type, boolean insertNodes);

    /**
     * Removes a node from the graph.
     *
     * @param node Node to remove.
     *
     * @return true if the edge is correctly removed, false if not.
     */
    default boolean removeNode(V node)
    {
        throw new UnsupportedOperationException("Deleting nodes is not allowed");
    }

    /**
     * Removes an edge from the graph.
     *
     * @param nodeA The incident node of the edge to remove.
     * @param nodeB The adjacent node of the edge to remove.
     *
     * @return true if everything went ok, false if not.
     */
    default boolean removeEdge(V nodeA, V nodeB)
    {
        throw new UnsupportedOperationException("Deleting edges is not allowed");
    }

    /**
     * Gets all the nodes in the graph.
     *
     * @return a stream of all the nodes in the graph.
     */
    Stream<V> getAllNodes();

    /**
     * Given a node, finds all the nodes u such that the edge (u to node) is in the graph.
     *
     * @param node The node.
     *
     * @return A stream of the incident nodes.
     */
    Stream<V> getIncidentNodes(V node);

    /**
     * Given a node, finds all the nodes u such that the edge (node to u) is in the graph.
     *
     * @param node The node.
     *
     * @return A stream containing the adjacent nodes.
     */
    Stream<V> getAdjacentNodes(V node);

    /**
     * Given a node, finds all the nodes u such that the edges (node to u) and (u to node) are
     * in the graph.
     *
     * @param node The node.
     *
     * @return A stream containing all the nodes which share reciprocal links.
     */
    Stream<V> getMutualNodes(V node);

    /**
     * Given a node, finds all the nodes u so that either (node to u) or (u to node) are in the graph.
     *
     * @param node The node.
     *
     * @return A stream containing all the nodes in the neighbourhood.
     */
    Stream<V> getNeighbourNodes(V node);

    /**
     * Gets all the nodes in the neighbourhood of a node given by a direction.
     *
     * @param node      The node.
     * @param direction The direction of the links.
     *
     * @return A stream containing the corresponding neighbourhood.
     */
    Stream<V> getNeighbourhood(V node, EdgeOrientation direction);

    /**
     * Calculates the degree of a node.
     *
     * @param node The node.
     *
     * @return the degree of the node if it is contained in the graph, -1 otherwise.
     */
    default int degree(V node)
    {
        if (!this.containsVertex(node))
        {
            return -1;
        }
        return this.getNeighbourEdgesCount(node);
    }

    /**
     * Calculates the in-degree of a node.
     *
     * @param node The node.
     *
     * @return the in-degree of the node if it is contained in the graph, -1 otherwise.
     */
    int inDegree(V node);

    /**
     * Calculates the out-degree of a node.
     *
     * @param node The node.
     *
     * @return the out-degree of the node if it is contained in the graph, -1 otherwise.
     */
    int outDegree(V node);

    /**
     * Obtains the degree of a node, depending on the neighborhood selection.
     *
     * @param node        The node whose degree we want to find.
     * @param orientation The neighborhood selection.
     *
     * @return the degree.
     */
    int degree(V node, EdgeOrientation orientation);

    /**
     * Calculates the number of incident edges of a node (not necessarily equal to the in-degree).
     *
     * @param node The node.
     *
     * @return the number of incident neighbours of the node if it is contained in the graph, -1 if not.
     */
    default int getIncidentNodesCount(V node)
    {
        if (!this.containsVertex(node))
        {
            return -1;
        }
        return (int) this.getIncidentNodes(node).count();
    }

    /**
     * Calculates the number of adjacent edges of a node (not necessarily equal to the out-degree).
     *
     * @param node The node
     *
     * @return the degree of the node if it is contained in the graph, -1 if not.
     */
    default int getAdjacentNodesCount(V node)
    {
        if (!this.containsVertex(node))
        {
            return -1;
        }
        return (int) this.getAdjacentNodes(node).count();
    }

    /**
     * Calculates the total number of edges which reach a node or start from it (not necessarily equal to the degree).
     *
     * @param node The node
     *
     * @return the degree of the node if it is contained in the graph, -1 if not.
     */
    default int getNeighbourNodesCount(V node)
    {
        if (!this.containsVertex(node))
        {
            return -1;
        }
        return (int) this.getNeighbourNodes(node).count();
    }

    /**
     * Calculates the number of nodes for which both (u to node) and (node to u)
     * links exist in the graph.
     *
     * @param node The node
     *
     * @return the number nodes for which both (u to node) and (node to u) exist in
     *         the graph if node is in the graph, -1 otherwise.
     */
    default int getMutualNodesCount(V node)
    {
        if (!this.containsVertex(node))
        {
            return -1;
        }
        return (int) this.getMutualNodes(node).count();
    }

    /**
     * Gets all the nodes in the neighbourhood of a node given by a direction.
     *
     * @param node      The node.
     * @param direction The direction of the links.
     *
     * @return A stream containing the corresponding neighbourhood.
     */
    int getNeighbourhoodSize(V node, EdgeOrientation direction);

    /**
     * Calculates the number of incident edges of a node (not necessarily equal to the in-degree).
     *
     * @param node The node.
     *
     * @return the number of incident neighbours of the node if it is contained in the graph, -1 if not.
     */
    int getIncidentEdgesCount(V node);

    /**
     * Calculates the number of adjacent edges of a node (not necessarily equal to the out-degree).
     *
     * @param node The node.
     *
     * @return the degree of the node if it is contained in the graph, -1 if not.
     */
    int getAdjacentEdgesCount(V node);

    /**
     * Calculates the total number of edges which reach a node or start from it (not necessarily equal to the degree).
     *
     * @param node The node.
     *
     * @return the degree of the node if it is contained in the graph, -1 if not.
     */
    int getNeighbourEdgesCount(V node);

    /**
     * Calculates the total number of adjacent edges of a node such that there is an
     * incident reciprocal link towards the node.
     *
     * @param node The node.
     *
     * @return the number of reciprocal links starting from the node.
     */
    int getMutualEdgesCount(V node);


    /**
     * Checks if a vertex exists in the graph.
     *
     * @param node The vertex to check.
     *
     * @return true if the vertex is contained in the graph, false if not.
     */
    boolean containsVertex(V node);

    /**
     * Checks if an edge exists in the graph.
     *
     * @param nodeA The incident node.
     * @param nodeB The adjacent node.
     *
     * @return true if the edge is contained in the graph, false if not.
     */
    boolean containsEdge(V nodeA, V nodeB);

    /**
     * Checks if an edge in the graph is mutual (a link from A to B and a link from
     * B to A exists).
     *
     * @param nodeA the first node.
     * @param nodeB the second node.
     *
     * @return true if the edge is mutual, false if not (at least one of the links is missing).
     */
    default boolean isMutual(V nodeA, V nodeB)
    {
        return this.containsEdge(nodeA, nodeB) && this.containsEdge(nodeB, nodeA);
    }

    /**
     * Obtains the weight of an edge in the graph
     *
     * @param nodeA The incident node.
     * @param nodeB The adjacent node.
     *
     * @return The corresponding weight. If the edge does not exist, NaN
     */
    double getEdgeWeight(V nodeA, V nodeB);

    /**
     * Updates the weight of an edge.
     *
     * @param nodeA     The incident node.
     * @param nodeB     The adjacent node.
     * @param newWeight The new weight.
     *
     * @return true if everything goes OK, false if the edge does not exist
     *         or something fails.
     */
    boolean updateEdgeWeight(V nodeA, V nodeB, double newWeight);


    /**
     * Given a node, finds the weights of the edges from the nodes u such that the edge (u to node) is in the graph.
     *
     * @param node The node.
     *
     * @return A stream containing the adjacent nodes and weights.
     */
    Stream<Weight<V, Double>> getIncidentNodesWeights(V node);

    /**
     * Given a node, finds the weights of the edges towards the nodes u such that the edge (node to u) is in the graph.
     *
     * @param node The node.
     *
     * @return A stream containing the adjacent nodes and weights.
     */
    Stream<Weight<V, Double>> getAdjacentNodesWeights(V node);

    /**
     * Given a node, finds the weights of the edges from the nodes u such that the edge (node to u) or the edge (u to node) are in the graph.
     *
     * @param node The node.
     *
     * @return A stream containing all the nodes in the neighbourhood and weights.
     */
    Stream<Weight<V, Double>> getNeighbourNodesWeights(V node);

    /**
     * Given a node, finds the weights of the edges towards the nodes u such that the edge (node to u) and the edge (u to node) are in the graph.
     *
     * @param node The node.
     *
     * @return A stream containing all the nodes in the neighbourhood and weights.
     */
    Stream<Weight<V, Double>> getAdjacentMutualNodesWeights(V node);

    /**
     * Given a node, finds the weights of the edges from the nodes u such that the edge (node to u) and the edge (u to node) are in the graph.
     *
     * @param node The node.
     *
     * @return A stream containing all the nodes in the neighbourhood and weights.
     */
    Stream<Weight<V, Double>> getIncidentMutualNodesWeights(V node);

    /**
     * Given a node, finds the weights of the edges towards and from the nodes u such that the edge (node to u) and the edge (u to node) are in the graph.
     * It finds the average value of the outgoing and incoming links.
     *
     * @param node The node.
     *
     * @return A stream containing all the nodes in the neighbourhood and weights.
     */
    Stream<Weight<V, Double>> getMutualNodesWeights(V node);

    /**
     * Gets all the weights of the edges in the neighbourhood of a node given by a direction.
     * In the mutual case, just returns the average of the edge weights.
     *
     * @param node      The node.
     * @param direction The direction of the links
     *
     * @return A stream containing the corresponding neighbourhood and weights.
     */
    Stream<Weight<V, Double>> getNeighbourhoodWeights(V node, EdgeOrientation direction);

    /**
     * Obtains the type of an edge in the graph
     *
     * @param nodeA The incident node.
     * @param nodeB The adjacent node.
     *
     * @return The corresponding type. If the edge does not exist, -1.
     */
    int getEdgeType(V nodeA, V nodeB);

    /**
     * Given a node, finds the types of the edges from the nodes u such that the edge (u to node) is in the graph.
     *
     * @param node The node.
     *
     * @return A stream of the incident nodes and types.
     */
    Stream<Weight<V, Integer>> getIncidentNodesTypes(V node);

    /**
     * Given a node, finds the types of the edges towards the nodes u such that the edge (node to u) is in the graph.
     *
     * @param node The node.
     *
     * @return A stream containing the adjacent nodes and types.
     */
    Stream<Weight<V, Integer>> getAdjacentNodesTypes(V node);

    /**
     * Given a node, finds the types of the edges from the nodes u such that the edge (node to u) or the edge (u to node) are in the graph.
     *
     * @param node The node.
     *
     * @return A stream containing all the nodes in the neighbourhood and types.
     */
    Stream<Weight<V, Integer>> getNeighbourNodesTypes(V node);

    /**
     * Given a node, finds the types of the edges towards the nodes u such that the edge (node to u) and the edge (u to node) are in the graph.
     *
     * @param node The node.
     *
     * @return A stream containing all the nodes in the neighbourhood and types.
     */
    Stream<Weight<V, Integer>> getAdjacentMutualNodesTypes(V node);

    /**
     * Given a node, finds the types of the edges from the nodes u such that the edge (node to u) and the edge (u to node) are in the graph.
     *
     * @param node The node.
     *
     * @return A stream containing all the nodes in the neighbourhood and types.
     */
    Stream<Weight<V, Integer>> getIncidentMutualNodesTypes(V node);

    /**
     * Gets all the types of the edges in the neighbourhood of a node given by a direction.
     * In the mutual case, it does not work (types are categorical values), so an empty
     * stream is returned.
     *
     * @param node      The node.
     * @param direction The direction of the links.
     *
     * @return A stream containing the corresponding neighbourhood.
     */
    Stream<Weight<V, Integer>> getNeighbourhoodTypes(V node, EdgeOrientation direction);

    /**
     * Indicates if the graph is directed.
     *
     * @return true if it is, false if not.
     */
    boolean isDirected();

    /**
     * Indicates if the graph is weighted.
     *
     * @return true if it is, false if not.
     */
    boolean isWeighted();

    /**
     * Indicates if the graph is a multigraph (by default, it is not).
     *
     * @return true if it is, false if not.
     */
    default boolean isMultigraph()
    {
        return false;
    }

    /**
     * Measures the number of nodes in the network.
     *
     * @return the number of nodes.
     */
    long getVertexCount();

    /**
     * Measures the number of edges in the network.
     *
     * @return the number of edges.
     */
    long getEdgeCount();

    /**
     * Gets the adjacency matrix.
     *
     * @param direction The direction of the edges.
     *
     * @return the adjacency matrix.
     */
    double[][] getAdjacencyMatrix(EdgeOrientation direction);

    /**
     * For an adjacency matrix, obtains the mapping between indexes and nodes.
     * @return the mapping between indexes and nodes.
     */
    Index<V> getAdjacencyMatrixMap();

    /**
     * Obtains the set of nodes without edges.
     *
     * @return the set of nodes without edges.
     */
    Stream<V> getIsolatedNodes();

    /**
     * Obtains the set of nodes with edges in a particular direction.
     *
     * @param direction the particular direction
     *
     * @return the set of nodes with edges in the given direction.
     */
    Stream<V> getNodesWithEdges(EdgeOrientation direction);

    /**
     * Obtains the set of nodes which have adjacent edges.
     *
     * @return the set of nodes which have adjacent edges.
     */
    Stream<V> getNodesWithAdjacentEdges();

    /**
     * Obtains the set of nodes which have incident edges.
     *
     * @return the set of nodes which have incident edges.
     */
    Stream<V> getNodesWithIncidentEdges();

    /**
     * Obtains the set of nodes having either incident or adjacent edges.
     *
     * @return the set of nodes which have incident or adjacent edges.
     */
    Stream<V> getNodesWithEdges();

    /**
     * Obtains the set of nodes having mutual edges.
     *
     * @return the set of nodes which have mutual edges.
     */
    Stream<V> getNodesWithMutualEdges();

    /**
     * Checks if the user has adjacent edges or not.
     *
     * @param u The user.
     *
     * @return true if the user has adjacent edges, false if it is a sink or isolated node.
     */
    boolean hasAdjacentEdges(V u);

    /**
     * Checks if the user has incident edges or not.
     *
     * @param u the user.
     *
     * @return true if the user has incident edges, false if it is a source or isolated node.
     */
    boolean hasIncidentEdges(V u);

    /**
     * Checks if the user shares at least an edge with other user.
     *
     * @param u The user.
     *
     * @return true if the user is not isolated, false otherwise.
     */
    boolean hasEdges(V u);

    /**
     * Checks if the user has mutual edges.
     *
     * @param u The user.
     *
     * @return true if the user has mutual edges, false otherwise.
     */
    boolean hasMutualEdges(V u);

    /**
     * Complements the graph
     *
     * @return the complementary graph.
     */
    Graph<V> complement();
}
