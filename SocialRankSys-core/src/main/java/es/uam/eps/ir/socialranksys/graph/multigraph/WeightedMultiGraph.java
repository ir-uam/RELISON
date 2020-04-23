/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph.multigraph;

import es.uam.eps.ir.socialranksys.graph.WeightedGraph;

import java.util.stream.Stream;


/**
 * Interface for directed graphs.
 * @author Javier Sanz-Cruzado Puig
 * @param <V> type of the vertices
 */
public interface WeightedMultiGraph<V> extends MultiGraph<V>, WeightedGraph<V>
{    
    /**
     * Gets the different weights for the edges of the incident nodes.
     * @param node The node to study
     * @return A stream containing the weights
     */
    @Override
    default Stream<Weights<V, Double>> getIncidentNodesWeightsLists(V node)
    {
        return this.getIncidentNodes(node).map((inc)-> new Weights<>(inc, this.getEdgeWeights(inc, inc)));
    }
    
    /**
     * Gets the different weights for the edges of the adjacent nodes.
     * @param node The node to study
     * @return A stream containing the weights
     */
    @Override
    default Stream<Weights<V, Double>> getAdjacentNodesWeightsLists(V node)
    {
        return this.getAdjacentNodes(node).map((adj) -> new Weights<>(adj, this.getEdgeWeights(node, adj)));
    }
    
    /**
     * Gets the different weights for the edges of the neighbour nodes.
     * @param node The node to study
     * @return A stream containing the weights
     */
    @Override
    default Stream<Weights<V, Double>> getNeighbourNodesWeightsLists(V node)
    {
        return this.getNeighbourNodes(node).map((adj)->new Weights<>(adj, this.getEdgeWeights(node, adj)));
    }
}
