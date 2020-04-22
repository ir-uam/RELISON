/* 
 *  Copyright (C) 2015 Information Retrieval Group at Universidad AutÃ³noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.graph.multigraph;


import es.uam.eps.ir.socialnetwork.graph.Weight;
import es.uam.eps.ir.socialnetwork.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialnetwork.graph.edges.EdgeWeight;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Interface for undirected unweighted multigraphs.
 * @author Javier Sanz-Cruzado Puig
 * @param <V> type of the vertices
 */
public interface UndirectedUnweightedMultiGraph<V> extends UnweightedMultiGraph<V>, UndirectedMultiGraph<V>
{   

    @Override
    default Stream<Weight<V,Double>> getIncidentNodesWeights(V node)
    {
        return getNeighbourNodesWeights(node);
    }

    @Override
    default Stream<Weight<V,Double>> getAdjacentNodesWeights(V node)
    {
        return getNeighbourNodesWeights(node);
    }

    @Override
    default Stream<Weight<V,Double>> getNeighbourNodesWeights(V node)
    {
        return this.getNeighbourNodes(node).map((neigh)->new Weight<>(neigh, EdgeWeight.getDefaultValue()));
    }

    @Override
    default Stream<Weight<V,Double>> getNeighbourhoodWeights(V node, EdgeOrientation direction)
    {
        return getNeighbourNodesWeights(node);
    }
    
    @Override
    default Stream<Weights<V,Double>> getIncidentNodesWeightsLists(V node)
    {
        return getNeighbourNodesWeightsLists(node);
    }

    @Override
    default Stream<Weights<V,Double>> getAdjacentNodesWeightsLists(V node)
    {
        return getNeighbourNodesWeightsLists(node);
    }

    /**
     * Gets the different weights for the edges of the neighbour nodes.
     * @param node The node to study
     * @return A stream containing the weights
     */
    @Override
    default Stream<Weights<V, Double>> getNeighbourNodesWeightsLists(V node)
    {
        return this.getNeighbourNodes(node).map((inc)-> 
        {
            List<Double> weights = new ArrayList<>();
            int numEdges = this.getNumEdges(inc, node);
            for(int i = 0; i < numEdges; ++i)
                weights.add(1.0);
            return new Weights<>(inc, weights);
        });
    }

    @Override
    default Stream<Weights<V,Double>> getNeighbourhoodWeightsLists(V node, EdgeOrientation direction)
    {
        return getNeighbourNodesWeightsLists(node);
    }
}
