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

import java.util.stream.Stream;

/**
 * Interface for undirected graphs.
 *
 * @param <V> Type of vertices.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface UndirectedGraph<V> extends Graph<V>
{
    /**
     * Given a node, finds all the nodes u such that the edge (u to node) is in the graph.
     *
     * @param node The node.
     *
     * @return A stream of the incident nodes.
     */
    @Override
    default Stream<V> getIncidentNodes(V node)
    {
        return getNeighbourNodes(node);
    }

    /**
     * Given a node, finds all the nodes u such that the edge (node to u) is in the graph.
     *
     * @param node The node.
     *
     * @return A stream containing the adjacent nodes.
     */
    @Override
    default Stream<V> getAdjacentNodes(V node)
    {
        return getNeighbourNodes(node);
    }

    /**
     * Given a node, finds all the nodes u such that the edges (node to u) and (u to node) are
     * in the graph.
     *
     * @param node The node.
     *
     * @return A stream containing all the nodes which share reciprocal links.
     */
    @Override
    default Stream<V> getMutualNodes(V node)
    {
        return getNeighbourNodes(node);
    }

    /**
     * Gets all the nodes in the neighbourhood of a node given by a direction.
     *
     * @param node      The node.
     * @param direction The direction of the links.
     *
     * @return A stream containing the corresponding neighbourhood.
     */
    @Override
    default Stream<V> getNeighbourhood(V node, EdgeOrientation direction)
    {
        return getNeighbourNodes(node);
    }

    /**
     * Calculates the number of incident neighbours of a node.
     *
     * @param node The node.
     *
     * @return the number of incident neighbours of the node if it is contained in the graph, -1 if not.
     */
    @Override
    default int getIncidentEdgesCount(V node)
    {
        return getNeighbourEdgesCount(node);
    }

    /**
     * Calculates the number of adjacent neighbours of a node.
     *
     * @param node The node.
     *
     * @return the degree of the node if it is contained in the graph, -1 if not.
     */
    @Override
    default int getAdjacentEdgesCount(V node)
    {
        return getNeighbourEdgesCount(node);
    }

    /**
     * Calculates the total number of adjacent edges of a node such that there is an
     * incident reciprocal link towards the node.
     *
     * @param node The node.
     *
     * @return the number of reciprocal links starting from the node.
     */
    @Override
    default int getMutualEdgesCount(V node)
    {
        return getNeighbourEdgesCount(node);
    }

    /**
     * Gets all the nodes in the neighbourhood of a node given by a direction.
     *
     * @param node      The node.
     * @param direction The direction of the links.
     *
     * @return A stream containing the corresponding neighbourhood.
     */
    @Override
    default int getNeighbourhoodSize(V node, EdgeOrientation direction)
    {
        return this.getNeighbourNodesCount(node);
    }

    /**
     * Calculates the number of nodes for which both (u to node) and (node to u)
     * links exist in the graph.
     *
     * @param node The node.
     *
     * @return the number nodes for which both (u to node) and (node to u) exist in
     *         the graph if node is in the graph, -1 otherwise.
     */
    @Override
    default int getMutualNodesCount(V node)
    {
        return getNeighbourNodesCount(node);
    }

    /**
     * Given a node, finds all the nodes u such that the edge (u to node) is in the graph.
     *
     * @param node The node.
     *
     * @return A stream of the incident nodes.
     */
    @Override
    default Stream<Weight<V, Double>> getIncidentNodesWeights(V node)
    {
        return this.getNeighbourNodesWeights(node);
    }

    /**
     * Given a node, finds all the nodes u such that the edge (node to u) is in the graph.
     *
     * @param node The node.
     *
     * @return A stream containing the adjacent nodes.
     */
    @Override
    default Stream<Weight<V, Double>> getAdjacentNodesWeights(V node)
    {
        return this.getNeighbourNodesWeights(node);
    }

    @Override
    default boolean isMutual(V nodeA, V nodeB)
    {
        return this.containsEdge(nodeA, nodeB);
    }

    /**
     * Gets all the nodes in the neighbourhood of a node given by a direction.
     *
     * @param node      The node.
     * @param direction The direction of the links.
     *
     * @return A stream containing the corresponding neighbourhood.
     */
    @Override
    default Stream<Weight<V, Double>> getNeighbourhoodWeights(V node, EdgeOrientation direction)
    {
        return this.getNeighbourNodesWeights(node);
    }

    @Override
    default int degree(V node)
    {
        return this.containsVertex(node) ? this.getNeighbourEdgesCount(node) : 0;
    }

    @Override
    default boolean isDirected()
    {
        return false;
    }

    @Override
    default Stream<Weight<V, Integer>> getIncidentNodesTypes(V node)
    {
        return this.getNeighbourNodesTypes(node);
    }

    @Override
    default Stream<Weight<V, Integer>> getAdjacentNodesTypes(V node)
    {
        return this.getNeighbourNodesTypes(node);
    }

    /**
     * Given a node, finds the types of the edges towards the nodes u such that the edge (node to u) and the edge (u to node) are in the graph.
     *
     * @param node The node.
     *
     * @return A stream containing all the nodes in the neighbourhood and types.
     */
    @Override
    default Stream<Weight<V, Integer>> getAdjacentMutualNodesTypes(V node)
    {
        return this.getNeighbourNodesTypes(node);
    }

    /**
     * Given a node, finds the types of the edges from the nodes u such that the edge (node to u) and the edge (u to node) are in the graph.
     *
     * @param node The node.
     *
     * @return A stream containing all the nodes in the neighbourhood and types.
     */
    @Override
    default Stream<Weight<V, Integer>> getIncidentMutualNodesTypes(V node)
    {
        return this.getNeighbourNodesTypes(node);
    }

    @Override
    default Stream<Weight<V, Integer>> getNeighbourhoodTypes(V node, EdgeOrientation direction)
    {
        return this.getNeighbourNodesTypes(node);
    }

    @Override
    default int getNeighbourEdgesCount(V node)
    {
        return (int) this.getNeighbourNodes(node).count();
    }

    @Override
    default int degree(V node, EdgeOrientation orientation)
    {
        return this.degree(node);
    }

    @Override
    default int inDegree(V node)
    {
        return this.degree(node);
    }

    @Override
    default int outDegree(V node)
    {
        return this.degree(node);
    }


}
