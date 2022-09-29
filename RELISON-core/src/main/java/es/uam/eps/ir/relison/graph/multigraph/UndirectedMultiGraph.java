/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.graph.multigraph;


import es.uam.eps.ir.relison.graph.UndirectedGraph;
import es.uam.eps.ir.relison.graph.Weight;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import org.jooq.lambda.tuple.Tuple2;

import java.util.stream.Stream;

/**
 * Interface for undirected graphs.
 *
 * @param <V> Type of the vertices.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface UndirectedMultiGraph<V> extends MultiGraph<V>, UndirectedGraph<V>
{
    /**
     * Gets the counts of neighbor edges between a node and its neighbors.
     *
     * @param node the node.
     * @return a stream containing a) the neighbor node and b) the number of edges between source and destination.
     */
    default Stream<Tuple2<V, Integer>> getNeighbourNodesCounts(V node)
    {
        return this.getAdjacentNodesCounts(node);
    }

    /**
     * Gets the counts of mutual edges between a node and its neighbors.
     * @param node the node.
     * @return a stream containing a) the mutual node and b) the number of edges between source and destination.
     */
    default Stream<Tuple2<V, Integer>> getMutualNodesCounts(V node)
    {
        return this.getAdjacentNodesCounts(node);
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

    @Override
    default Stream<Weight<V, Integer>> getNeighbourhoodTypes(V node, EdgeOrientation direction)
    {
        return this.getNeighbourNodesTypes(node);
    }

    @Override
    default Stream<Weights<V, Integer>> getIncidentNodesTypesLists(V node)
    {
        return this.getNeighbourNodesTypesLists(node);
    }

    @Override
    default Stream<Weights<V, Integer>> getAdjacentNodesTypesLists(V node)
    {
        return this.getNeighbourNodesTypesLists(node);
    }

    @Override
    default Stream<Weights<V, Integer>> getNeighbourhoodTypesLists(V node, EdgeOrientation direction)
    {
        return this.getNeighbourNodesTypesLists(node);
    }

    @Override
    default int degree(V node)
    {
        return this.getNeighbourEdgesCount(node);
    }

}
