/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.graph;

import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;

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
public interface DirectedGraph<V> extends Graph<V>
{
    @Override
    default Stream<V> getNeighbourhood(V node, EdgeOrientation direction)
    {
        return switch (direction)
        {
            case OUT -> this.getAdjacentNodes(node);
            case IN -> this.getIncidentNodes(node);
            case UND -> this.getNeighbourNodes(node);
            case MUTUAL -> this.getMutualNodes(node);
        };
    }

    @Override
    default int getNeighbourhoodSize(V node, EdgeOrientation direction)
    {
        return switch (direction)
        {
            case OUT -> this.getAdjacentNodesCount(node);
            case IN -> this.getIncidentNodesCount(node);
            case UND -> this.getNeighbourNodesCount(node);
            case MUTUAL -> this.getMutualNodesCount(node);
        };
    }

    @Override
    default Stream<Weight<V, Double>> getNeighbourhoodWeights(V node, EdgeOrientation direction)
    {
        return switch (direction)
        {
            case OUT -> this.getAdjacentNodesWeights(node);
            case IN -> this.getIncidentNodesWeights(node);
            case UND -> this.getNeighbourNodesWeights(node);
            case MUTUAL -> this.getMutualNodesWeights(node);
        };
    }

    @Override
    default Stream<Weight<V, Integer>> getNeighbourhoodTypes(V node, EdgeOrientation direction)
    {
        return switch (direction)
        {
            case OUT -> this.getAdjacentNodesTypes(node);
            case IN -> this.getIncidentNodesTypes(node);
            case UND -> this.getNeighbourNodesTypes(node);
            case MUTUAL -> this.getMutualNodesTypes(node);
        };
    }

    @Override
    default int degree(V node)
    {
        if (this.containsVertex(node))
        {
            return this.inDegree(node) + this.outDegree(node);
        }
        return 0;
    }

    /**
     * Obtains the in-degree of a node.
     *
     * @param node The node.
     *
     * @return the in-degree of the node.
     */
    @Override
    default int inDegree(V node)
    {
        return this.containsVertex(node) ? this.getIncidentEdgesCount(node) : -1;
    }

    /**
     * Obtains the out-degree of a node.
     *
     * @param node The node.
     *
     * @return the out-degree of the node.
     */
    @Override
    default int outDegree(V node)
    {
        return this.containsVertex(node) ? this.getAdjacentEdgesCount(node) : -1;
    }

    /**
     * Obtains the mutual degree of a node.
     *
     * @param node the node.
     * @return the mutual degree of the node
     */
    @Override
    default int mutualDegree(V node)
    {
        return this.containsVertex(node) ? this.getMutualEdgesCount(node) : -1;
    }

    @Override
    default int degree(V node, EdgeOrientation orientation)
    {
        if (!this.containsVertex(node))
        {
            return -1;
        }
        return switch (orientation)
        {
            case IN -> this.inDegree(node);
            case OUT -> this.outDegree(node);
            case UND -> this.inDegree(node) + this.outDegree(node);
            case MUTUAL -> this.mutualDegree(node);
        };
    }

    @Override
    default int getNeighbourEdgesCount(V node)
    {
        return this.getIncidentEdgesCount(node) + this.getAdjacentEdgesCount(node);
    }

    @Override
    default boolean isDirected()
    {
        return true;
    }

    /**
     * Gets all the nodes which just have outgoing links.
     *
     * @return the set of nodes with just outgoing links.
     */
    default Stream<V> getSources()
    {
        return this.getNodesWithAdjacentNeighbors().filter(u -> !this.hasIncidentNeighbors(u));
    }

    /**
     * Gets all the nodes which just have incoming links.
     *
     * @return the set of nodes with just incoming links.
     */
    default Stream<V> getSinks()
    {
        return this.getNodesWithIncidentNeighbors().filter(u -> !this.hasAdjacentNeighbors(u));
    }
}
