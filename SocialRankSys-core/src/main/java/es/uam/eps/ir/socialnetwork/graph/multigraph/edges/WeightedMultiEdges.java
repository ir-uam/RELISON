/* 
 *  Copyright (C) 2015 Information Retrieval Group at Universidad AutÃ³noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.graph.multigraph.edges;

import java.util.stream.Stream;

/**
 * Interface for weighted edges
 * @author Javier Sanz-Cruzado Puig
 */
public interface WeightedMultiEdges extends MultiEdges
{

    /**
     * Given a node, finds all the weights of edges such that the edge (u to node) is in the graph.
     * @param node The node.
     * @return A stream of the weights of incident nodes.
     */
    @Override
    default Stream<MultiEdgeWeights> getIncidentWeight(int node)
    {
        return this.getIncidentNodes(node).map((inc) -> new MultiEdgeWeights(inc, this.getEdgeWeights(inc, node)));
    }
    
    /**
     * Given a node, finds all the weights of edges u such that the edge (node to u) is in the graph.
     * @param node The node
     * @return A stream containing the weights adjacent nodes.
     */
    @Override
    default Stream<MultiEdgeWeights> getAdjacentWeight(int node)
    {
        return this.getAdjacentNodes(node).map((inc) -> new MultiEdgeWeights(inc, this.getEdgeWeights(node, inc)));
    }
    
    /**
     * Given a node, finds all the all the weights of edges so that either (node to u) or (u to node) are in the graph.
     * @param node The node
     * @return A stream containing all the weights of the nodes in the neighbourhood.
     */
    @Override
    default Stream<MultiEdgeWeights> getNeighbourWeight(int node)
    {
        return this.getNeighbourNodes(node).map((inc) -> new MultiEdgeWeights(inc, this.getEdgeWeights(node, inc)));
    }
}
