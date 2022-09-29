/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.graph.multigraph;


import es.uam.eps.ir.relison.graph.DirectedGraph;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import org.jooq.lambda.tuple.Tuple2;

import java.util.stream.Stream;

/**
 * Interface for directed multi graphs.
 *
 * @param <U> type of the vertices
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface DirectedMultiGraph<U> extends MultiGraph<U>, DirectedGraph<U>
{
    /**
     * Gets the counts of neighbor edges between a node and its neighbors.
     *
     * @param node the node.
     * @return a stream containing a) the neighbor node and b) the number of edges between source and destination.
     */
    default Stream<Tuple2<U, Integer>> getNeighbourNodesCounts(U node)
    {
        return this.getNeighbourNodes(node).map(v -> new Tuple2<>(v, this.getNumEdges(node, v) + this.getNumEdges(v, node)));
    }

    /**
     * Gets the counts of mutual edges between a node and its neighbors.
     * @param node the node.
     * @return a stream containing a) the mutual node and b) the number of edges between source and destination.
     */
    default Stream<Tuple2<U, Integer>> getMutualNodesCounts(U node)
    {
        return this.getMutualNodes(node).map(v -> new Tuple2<>(v, this.getNumEdges(node,v) + this.getNumEdges(v, node)));
    }

    @Override
    default Stream<Weights<U, Double>> getNeighbourhoodWeightsLists(U node, EdgeOrientation direction)
    {
        return switch (direction)
        {
            case OUT -> this.getAdjacentNodesWeightsLists(node);
            case IN -> this.getIncidentNodesWeightsLists(node);
            case UND -> this.getNeighbourNodesWeightsLists(node);
            default -> Stream.empty();
        };
    }

    @Override
    default Stream<Weights<U, Integer>> getNeighbourhoodTypesLists(U node, EdgeOrientation direction)
    {
        return switch (direction)
        {
            case OUT -> this.getAdjacentNodesTypesLists(node);
            case IN -> this.getIncidentNodesTypesLists(node);
            case UND -> this.getNeighbourNodesTypesLists(node);
            default -> Stream.empty();
        };
    }

    @Override
    default int inDegree(U node)
    {
        return this.getIncidentEdgesCount(node);
    }

    @Override
    default int outDegree(U node)
    {
        return this.getAdjacentEdgesCount(node);
    }
}
