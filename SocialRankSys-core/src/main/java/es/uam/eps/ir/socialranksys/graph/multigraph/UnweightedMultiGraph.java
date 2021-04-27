/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph.multigraph;

import es.uam.eps.ir.socialranksys.graph.UnweightedGraph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeWeight;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Interface for directed graphs.
 *
 * @param <V> type of the vertices
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface UnweightedMultiGraph<V> extends MultiGraph<V>, UnweightedGraph<V>
{
    /**
     * Gets the different weights for the edges of the incident nodes.
     *
     * @param node The node to study
     *
     * @return A stream containing the weights
     */
    @Override
    default Stream<Weights<V, Double>> getIncidentNodesWeightsLists(V node)
    {
        return this.getIncidentNodes(node).map((inc) ->
        {
            List<Double> weights = new ArrayList<>();
            int numEdges = this.getNumEdges(inc, node);
            for (int i = 0; i < numEdges; ++i)
            {
                weights.add(1.0);
            }
            return new Weights<>(inc, weights);
        });
    }

    /**
     * Gets the different weights for the edges of the adjacent nodes.
     *
     * @param node The node to study
     *
     * @return A stream containing the weights
     */
    @Override
    default Stream<Weights<V, Double>> getAdjacentNodesWeightsLists(V node)
    {
        return this.getAdjacentNodes(node).map((adj) ->
        {
            List<Double> weights = new ArrayList<>();
            int numEdges = this.getNumEdges(node, adj);
            for (int i = 0; i < numEdges; ++i)
            {
                weights.add(1.0);
            }
            return new Weights<>(adj, weights);
        });
    }

    /**
     * Gets the different weights for the edges of the neighbour nodes.
     *
     * @param node The node to study
     *
     * @return A stream containing the weights
     */
    @Override
    default Stream<Weights<V, Double>> getNeighbourNodesWeightsLists(V node)
    {
        return this.getNeighbourNodes(node).map((inc) ->
        {
            List<Double> weights = new ArrayList<>();
            int numEdges = this.getNumEdges(inc, node);
            for (int i = 0; i < numEdges; ++i)
            {
                weights.add(1.0);
            }
            return new Weights<>(inc, weights);
        });
    }


    @Override
    default List<Double> getEdgeWeights(V incident, V adjacent)
    {
        int numEdges = this.getNumEdges(incident, adjacent);
        List<Double> edges = new ArrayList<>();
        if (numEdges > 0)
        {
            for (int i = 0; i < numEdges; ++i)
            {
                edges.add(EdgeWeight.getDefaultValue());
            }
        }
        return edges;
    }
}
