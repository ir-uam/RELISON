/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph.multigraph;


import es.uam.eps.ir.socialranksys.graph.UndirectedGraph;
import es.uam.eps.ir.socialranksys.graph.Weight;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;

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


}
