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
 * Interface for undirected weighted graphs.
 *
 * @param <V> Type of vertices.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface UndirectedWeightedGraph<V> extends WeightedGraph<V>, UndirectedGraph<V>
{

    @Override
    default Stream<Weight<V, Double>> getIncidentNodesWeights(V node)
    {
        return getNeighbourNodesWeights(node);
    }

    @Override
    default Stream<Weight<V, Double>> getAdjacentNodesWeights(V node)
    {
        return getNeighbourNodesWeights(node);
    }

    @Override
    default Stream<Weight<V, Double>> getNeighbourNodesWeights(V node)
    {
        return this.getNeighbourNodes(node).map((neigh) -> new Weight<>(neigh, this.getEdgeWeight(node, neigh)));
    }

    @Override
    default Stream<Weight<V, Double>> getNeighbourhoodWeights(V node, EdgeOrientation direction)
    {
        return getNeighbourNodesWeights(node);
    }
}
