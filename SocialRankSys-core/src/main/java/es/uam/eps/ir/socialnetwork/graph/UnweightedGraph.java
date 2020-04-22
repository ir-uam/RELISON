/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.graph;


import es.uam.eps.ir.socialnetwork.graph.edges.EdgeWeight;

import java.util.stream.Stream;

/**
 * Interface for directed graphs.
 *
 * @param <V> Type of vertices.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface UnweightedGraph<V> extends Graph<V>
{
    /**
     * Given a node, finds all the nodes u such that the edge (u to node) is in the graph.
     *
     * @param node The node.
     *
     * @return a stream of the incident nodes.
     */
    @Override
    default Stream<Weight<V, Double>> getIncidentNodesWeights(V node)
    {
        return this.getIncidentNodes(node).map((inc) -> new Weight<>(inc, EdgeWeight.getDefaultValue()));
    }

    /**
     * Given a node, finds all the nodes u such that the edge (node to u) is in the graph.
     *
     * @param node The node.
     *
     * @return a stream containing the adjacent nodes.
     */
    @Override
    default Stream<Weight<V, Double>> getAdjacentNodesWeights(V node)
    {
        return this.getAdjacentNodes(node).map((inc) -> new Weight<>(inc, EdgeWeight.getDefaultValue()));
    }

    /**
     * Given a node, finds all the nodes u so that either (node to u) or (u to node) are in the graph.
     *
     * @param node The node.
     *
     * @return a stream containing all the nodes in the neighbourhood.
     */
    @Override
    default Stream<Weight<V, Double>> getNeighbourNodesWeights(V node)
    {
        return this.getNeighbourNodes(node).map((inc) -> new Weight<>(inc, EdgeWeight.getDefaultValue()));
    }

    /**
     * Given a node, finds all the nodes u so that either (node to u) or (u to node) are in the graph.
     *
     * @param node The node.
     *
     * @return a stream containing all the nodes in the neighbourhood.
     */
    @Override
    default Stream<Weight<V, Double>> getMutualNodesWeights(V node)
    {
        return this.getMutualNodes(node).map((inc) -> new Weight<>(inc, EdgeWeight.getDefaultValue()));
    }

    /**
     * Given a node, finds all the nodes u so that either (node to u) or (u to node) are in the graph.
     *
     * @param node The node.
     *
     * @return A stream containing all the nodes in the neighbourhood.
     */
    @Override
    default Stream<Weight<V, Double>> getAdjacentMutualNodesWeights(V node)
    {
        return this.getMutualNodes(node).map((inc) -> new Weight<>(inc, EdgeWeight.getDefaultValue()));
    }

    /**
     * Given a node, finds all the nodes u so that either (node to u) or (u to node) are in the graph.
     *
     * @param node The node.
     *
     * @return a stream containing all the nodes in the neighbourhood.
     */
    @Override
    default Stream<Weight<V, Double>> getIncidentMutualNodesWeights(V node)
    {
        return this.getMutualNodes(node).map((inc) -> new Weight<>(inc, EdgeWeight.getDefaultValue()));
    }

    @Override
    default double getEdgeWeight(V incident, V adjacent)
    {
        if (this.containsEdge(incident, adjacent))
        {
            return EdgeWeight.getDefaultValue();
        }
        else
        {
            return EdgeWeight.getErrorValue();
        }
    }

    @Override
    default boolean addEdge(V nodeA, V nodeB, double weight, int type)
    {
        return this.addEdge(nodeA, nodeB, EdgeWeight.getDefaultValue(), type, true);
    }

    @Override
    default boolean isWeighted()
    {
        return false;
    }

}
