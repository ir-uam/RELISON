/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph.multigraph.edges;

import es.uam.eps.ir.socialranksys.utils.listcombiner.OrderedListCombiner;

import java.util.Comparator;
import java.util.List;
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
    default int getNeighbourCount(int node)
    {
        return this.getAdjacentCount(node) + this.getIncidentCount(node);
    }



}
