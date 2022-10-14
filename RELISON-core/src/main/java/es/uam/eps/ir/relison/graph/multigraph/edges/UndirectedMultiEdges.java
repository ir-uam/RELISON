/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.graph.multigraph.edges;

import es.uam.eps.ir.relison.graph.edges.fast.FastEdge;
import es.uam.eps.ir.relison.graph.multigraph.edges.fast.FastMultiEdge;

import java.util.ArrayList;
import java.util.List;
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
    default Stream<MultiEdgeWeights> getMutualAdjacentWeights(int node)
    {
        return this.getNeighbourWeights(node);
    }

    @Override
    default Stream<MultiEdgeWeights> getMutualIncidentWeights(int node)
    {
        return this.getNeighbourWeights(node);
    }

    @Override
    default Stream<MultiEdgeTypes> getMutualAdjacentTypes(int node)
    {
        return this.getNeighbourTypes(node);
    }

    @Override
    default Stream<MultiEdgeTypes> getMutualIncidentTypes(int node)
    {
        return this.getNeighbourTypes(node);
    }

    /*@Override
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
    }*/

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

    @Override
    default Stream<FastMultiEdge> getAdjacentEdges(int idx)
    {
        return this.getNeighbourEdges(idx);
    }

    @Override
    default Stream<FastMultiEdge> getIncidentEdges(int idx)
    {
        return this.getNeighbourEdges(idx);
    }

    @Override
    default Stream<FastMultiEdge> getMutualEdges(int idx)
    {
        return this.getNeighbourEdges(idx);
    }

    @Override
    default Stream<FastMultiEdge> getNeighbourEdges(int idx)
    {
        List<FastMultiEdge> fastEdges = new ArrayList<>();
        this.getNeighbourNodes(idx).forEach(vidx ->
        {
            List<Double> weights = this.getEdgeWeights(idx, vidx);
            List<Integer> types = this.getEdgeTypes(idx, vidx);

            IntStream.range(0, weights.size()).forEach(i -> fastEdges.add(new FastMultiEdge(idx, vidx, weights.get(i), types.get(i), i)));
        });

        return fastEdges.stream();
    }

    @Override
    default Stream<FastMultiEdge> getMutualAdjacentEdges(int idx)
    {
        return this.getNeighbourEdges(idx);
    }

    @Override
    default Stream<FastMultiEdge> getMutualIncidentEdges(int idx)
    {
        return this.getNeighbourEdges(idx);
    }
}
