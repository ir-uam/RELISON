/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph.multigraph.edges;

import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Class for the undirected multi-edges.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface UndirectedMultiEdges extends MultiEdges
{
    @Override
    default Stream<Integer> getIncidentNodes(int node)
    {
        return this.getNeighbourNodes(node);
    }

    @Override
    default Stream<Integer> getAdjacentNodes(int node)
    {
        return this.getNeighbourNodes(node);
    }

    @Override
    default Stream<Integer> getMutualNodes(int node) {
        return this.getNeighbourNodes(node);
    }

    @Override
    default Stream<MultiEdgeTypes> getIncidentTypes(int node)
    {
        return this.getNeighbourTypes(node);
    }

    @Override
    default Stream<MultiEdgeTypes> getAdjacentTypes(int node)
    {
        return this.getNeighbourTypes(node);
    }

    @Override
    default Stream<MultiEdgeTypes> getMutualTypes(int node)
    {
        return this.getNeighbourTypes(node);
    }

    @Override
    default Stream<MultiEdgeWeights> getIncidentWeights(int node)
    {
        return this.getNeighbourWeights(node);
    }

    @Override
    default Stream<MultiEdgeWeights> getAdjacentWeights(int node)
    {
        return this.getNeighbourWeights(node);
    }

    @Override
    default Stream<MultiEdgeWeights> getMutualWeights(int node)
    {
        return this.getNeighbourWeights(node);
    }

    @Override
    default int getNeighbourCount(int node)
    {
        return this.getIncidentCount(node);
    }

    @Override
    default IntStream getNodesWithIncidentEdges()
    {
        return this.getNodesWithEdges();
    }

    @Override
    default IntStream getNodesWithAdjacentEdges()
    {
        return this.getNodesWithEdges();
    }


    @Override
    default IntStream getNodesWithMutualEdges()
    {
        return this.getNodesWithEdges();
    }
}
