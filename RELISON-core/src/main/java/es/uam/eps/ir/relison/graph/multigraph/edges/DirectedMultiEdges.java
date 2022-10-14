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
import es.uam.eps.ir.relison.utils.listcombiner.OrderedListCombiner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Class for the directed multi-edges.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface DirectedMultiEdges extends MultiEdges
{
    @Override
    default Stream<Integer> getNeighbourNodes(int node)
    {
        Stream<Integer> incident = this.getIncidentNodes(node);
        Stream<Integer> adjacent = this.getAdjacentNodes(node);
        List<Integer> list = OrderedListCombiner.mergeLists(incident, adjacent, Comparator.naturalOrder(), (x, y) -> x);
        return list.stream();
    }

    @Override
    default Stream<Integer> getMutualNodes(int node)
    {
        Stream<Integer> incident = this.getIncidentNodes(node);
        Stream<Integer> adjacent = this.getAdjacentNodes(node);
        List<Integer> list = OrderedListCombiner.intersectLists(incident, adjacent, Comparator.naturalOrder(), (x, y) -> x);
        return list.stream();
    }

    @Override
    default Stream<MultiEdgeTypes> getNeighbourTypes(int node)
    {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    default Stream<MultiEdgeTypes> getMutualTypes(int node)
    {
        throw new UnsupportedOperationException("Not supported");
    }

    /*@Override
    default Stream<MultiEdgeWeights> getNeighbourWeights(int node)
    {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    default Stream<MultiEdgeWeights> getMutualWeights(int node)
    {
        throw new UnsupportedOperationException("Not supported");
    }*/

    @Override
    default Stream<MultiEdgeWeights> getMutualAdjacentWeights(int node)
    {
        return this.getMutualNodes(node).map(v -> new MultiEdgeWeights(v, this.getEdgeWeights(node, v)));
    }

    @Override
    default Stream<MultiEdgeWeights> getMutualIncidentWeights(int node)
    {
        return this.getMutualNodes(node).map(v -> new MultiEdgeWeights(v, this.getEdgeWeights(v, node)));
    }

    @Override
    default Stream<MultiEdgeTypes> getMutualAdjacentTypes(int node)
    {
        return this.getMutualNodes(node).map(v -> new MultiEdgeTypes(v, this.getEdgeTypes(node, v)));
    }

    @Override
    default Stream<MultiEdgeTypes> getMutualIncidentTypes(int node)
    {
        return this.getMutualNodes(node).map(v -> new MultiEdgeTypes(v, this.getEdgeTypes(v, node)));
    }

    @Override
    default int getNeighbourCount(int node)
    {
        return this.getAdjacentCount(node) + this.getIncidentCount(node);
    }

    @Override
    default Stream<FastMultiEdge> getAdjacentEdges(int idx)
    {
        List<FastMultiEdge> fastEdges = new ArrayList<>();
        this.getAdjacentNodes(idx).forEach(vidx ->
        {
            List<Double> weights = this.getEdgeWeights(idx, vidx);
            List<Integer> types = this.getEdgeTypes(idx, vidx);

            IntStream.range(0, weights.size()).forEach(i -> fastEdges.add(new FastMultiEdge(idx, vidx, weights.get(i), types.get(i), i)));
        });

        return fastEdges.stream();
    }

    @Override
    default Stream<FastMultiEdge> getIncidentEdges(int idx)
    {
        List<FastMultiEdge> fastEdges = new ArrayList<>();
        this.getIncidentNodes(idx).forEach(vidx ->
        {
            List<Double> weights = this.getEdgeWeights(idx, vidx);
            List<Integer> types = this.getEdgeTypes(idx, vidx);

            IntStream.range(0, weights.size()).forEach(i -> fastEdges.add(new FastMultiEdge(vidx, idx, weights.get(i), types.get(i), i)));
        });

        return fastEdges.stream();
    }

    @Override
    default Stream<FastMultiEdge> getMutualEdges(int idx)
    {
        return Stream.concat(this.getMutualAdjacentEdges(idx), this.getMutualIncidentEdges(idx));
    }

    @Override
    default Stream<FastMultiEdge> getNeighbourEdges(int idx)
    {
        return Stream.concat(this.getAdjacentEdges(idx), this.getIncidentEdges(idx));
    }

    @Override
    default Stream<FastMultiEdge> getMutualAdjacentEdges(int idx)
    {
        List<FastMultiEdge> fastEdges = new ArrayList<>();
        this.getAdjacentNodes(idx).forEach(vidx ->
        {
            List<Double> weights = this.getEdgeWeights(idx, vidx);
            List<Integer> types = this.getEdgeTypes(idx, vidx);

            IntStream.range(0, weights.size()).forEach(i -> fastEdges.add(new FastMultiEdge(idx, vidx, weights.get(i), types.get(i), i)));
        });

        return fastEdges.stream();
    }

    @Override
    default Stream<FastMultiEdge> getMutualIncidentEdges(int idx)
    {
        List<FastMultiEdge> fastEdges = new ArrayList<>();
        this.getAdjacentNodes(idx).forEach(vidx ->
        {
            List<Double> weights = this.getEdgeWeights(vidx, idx);
            List<Integer> types = this.getEdgeTypes(vidx, idx);

            IntStream.range(0, weights.size()).forEach(i -> fastEdges.add(new FastMultiEdge(vidx, idx, weights.get(i), types.get(i), i)));
        });

        return fastEdges.stream();
    }

}
