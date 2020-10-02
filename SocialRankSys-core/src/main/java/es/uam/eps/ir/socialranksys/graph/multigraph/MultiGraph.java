/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph.multigraph;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;

import java.util.List;
import java.util.stream.Stream;

/**
 * Interface for representing multigraphs
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface MultiGraph<U> extends Graph<U>
{

    /**
     * Gets the number of edges between two nodes, A and B.
     *
     * @param nodeA The first node of the pair.
     * @param nodeB The second node of the pair.
     *
     * @return The number of edges between the nodes.
     */
    int getNumEdges(U nodeA, U nodeB);

    /**
     * Gets the weights of the different edges between two nodes, A and B.
     *
     * @param nodeA The first node of the pair.
     * @param nodeB THe second node of the pair.
     *
     * @return The number of edges between the nodes.
     */
    List<Double> getEdgeWeights(U nodeA, U nodeB);


    /**
     * Gets the different weights for the edges of the incident nodes.
     *
     * @param node The node to study
     *
     * @return A stream containing the weights
     */
    Stream<Weights<U, Double>> getIncidentNodesWeightsLists(U node);

    /**
     * Gets the different weights for the edges of the adjacent nodes.
     *
     * @param node The node to study
     *
     * @return A stream containing the weights
     */
    Stream<Weights<U, Double>> getAdjacentNodesWeightsLists(U node);

    /**
     * Gets the different weights for the edges of the neighbour nodes.
     *
     * @param node The node to study
     *
     * @return A stream containing the weights
     */
    Stream<Weights<U, Double>> getNeighbourNodesWeightsLists(U node);


    /**
     * Gets the different weights for the edges of the selected neighbour nodes.
     *
     * @param node        The node to study
     * @param orientation The orientation to take
     *
     * @return A stream containing the weights
     */
    Stream<Weights<U, Double>> getNeighbourhoodWeightsLists(U node, EdgeOrientation orientation);

    /**
     * Gets the weights of the different edges between two nodes, A and B.
     *
     * @param nodeA The first node of the pair.
     * @param nodeB THe second node of the pair.
     *
     * @return The number of edges between the nodes.
     */
    List<Integer> getEdgeTypes(U nodeA, U nodeB);


    /**
     * Gets the different weights for the edges of the incident nodes.
     *
     * @param node The node to study
     *
     * @return A stream containing the weights
     */
    Stream<Weights<U, Integer>> getIncidentNodesTypesLists(U node);

    /**
     * Gets the different weights for the edges of the adjacent nodes.
     *
     * @param node The node to study
     *
     * @return A stream containing the weights
     */
    Stream<Weights<U, Integer>> getAdjacentNodesTypesLists(U node);

    /**
     * Gets the different weights for the edges of the neighbour nodes.
     *
     * @param node The node to study
     *
     * @return A stream containing the weights
     */
    Stream<Weights<U, Integer>> getNeighbourNodesTypesLists(U node);


    /**
     * Gets the different weights for the edges of the selected neighbour nodes.
     *
     * @param node        The node to study
     * @param orientation The orientation to take
     *
     * @return A stream containing the weights
     */
    Stream<Weights<U, Integer>> getNeighbourhoodTypesLists(U node, EdgeOrientation orientation);

    @Override
    default boolean isMultigraph()
    {
        return true;
    }
}
