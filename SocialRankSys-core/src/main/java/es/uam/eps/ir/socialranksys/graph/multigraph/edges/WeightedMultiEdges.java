/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph.multigraph.edges;

import java.util.stream.Stream;

/**
 * Interface for weighted edges
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface WeightedMultiEdges extends MultiEdges
{

    /**
     * Given a node, finds all the weights of edges such that the edge (u to node) is in the graph.
     *
     * @param node The node.
     *
     * @return A stream of the weights of incident nodes.
     */
    @Override
    default Stream<MultiEdgeWeights> getIncidentWeights(int node)
    {
        return this.getIncidentNodes(node).map((inc) -> new MultiEdgeWeights(inc, this.getEdgeWeights(inc, node)));
    }

    /**
     * Given a node, finds all the weights of edges u such that the edge (node to u) is in the graph.
     *
     * @param node The node
     *
     * @return A stream containing the weights adjacent nodes.
     */
    @Override
    default Stream<MultiEdgeWeights> getAdjacentWeights(int node)
    {
        return this.getAdjacentNodes(node).map((inc) -> new MultiEdgeWeights(inc, this.getEdgeWeights(node, inc)));
    }

    /**
     * Given a node, finds all the all the weights of edges so that either (node to u) or (u to node) are in the graph.
     *
     * @param node The node
     *
     * @return A stream containing all the weights of the nodes in the neighbourhood.
     */
    @Override
    default Stream<MultiEdgeWeights> getNeighbourWeights(int node)
    {
        return this.getNeighbourNodes(node).map((inc) -> new MultiEdgeWeights(inc, this.getEdgeWeights(node, inc)));
    }

    /**
     * Given a node, finds all the all the weights of edges so that both (node to u) and (u to node) are in the graph.
     *
     * @param node The node
     *
     * @return A stream containing all the weights of the nodes in the neighbourhood.
     */
    @Override
    default Stream<MultiEdgeWeights> getMutualWeights(int node)
    {
        return this.getMutualNodes(node).map((inc) -> new MultiEdgeWeights(inc, this.getEdgeWeights(node, inc)));
    }
}
