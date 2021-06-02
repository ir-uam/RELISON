/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.graph.edges;

import es.uam.eps.ir.ranksys.fast.preference.IdxPref;
import es.uam.eps.ir.sonalire.index.IdxValue;
import es.uam.eps.ir.sonalire.utils.listcombiner.OrderedListCombiner;

import java.util.Comparator;
import java.util.Iterator;
import java.util.stream.Stream;

/**
 * Interface for the directed edges.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface DirectedEdges extends Edges
{
    @Override
    default Stream<Integer> getNeighbourNodes(int node)
    {
        Iterator<Integer> iteratorIncident = this.getIncidentNodes(node).iterator();
        Iterator<Integer> iteratorAdjacent = this.getAdjacentNodes(node).iterator();
        return OrderedListCombiner.mergeLists(iteratorIncident, iteratorAdjacent, Comparator.naturalOrder(), (x, y) -> x).stream();
    }

    @Override
    default Stream<Integer> getMutualNodes(int node)
    {
        Iterator<Integer> iteratorIncident = this.getIncidentNodes(node).iterator();
        Iterator<Integer> iteratorAdjacent = this.getAdjacentNodes(node).iterator();
        return OrderedListCombiner.intersectLists(iteratorIncident, iteratorAdjacent, Comparator.naturalOrder(), (x, y) -> x).stream();
    }

    @Override
    default Stream<IdxPref> getMutualAdjacentWeights(int node)
    {
        Iterator<IdxPref> iteratorIncident = this.getIncidentWeights(node).iterator();
        Iterator<IdxPref> iteratorAdjacent = this.getAdjacentWeights(node).iterator();
        return OrderedListCombiner.intersectLists(iteratorIncident, iteratorAdjacent, Comparator.comparingInt(x -> x.v1), (x, y) -> y).stream();
    }

    @Override
    default Stream<IdxPref> getMutualIncidentWeights(int node)
    {
        Iterator<IdxPref> iteratorIncident = this.getIncidentWeights(node).iterator();
        Iterator<IdxPref> iteratorAdjacent = this.getAdjacentWeights(node).iterator();
        return OrderedListCombiner.intersectLists(iteratorIncident, iteratorAdjacent, Comparator.comparingInt(x -> x.v1), (x, y) -> x).stream();
    }

    @Override
    default Stream<IdxPref> getMutualWeights(int node)
    {
        Iterator<IdxPref> iteratorIncident = this.getIncidentWeights(node).iterator();
        Iterator<IdxPref> iteratorAdjacent = this.getAdjacentWeights(node).iterator();

        return OrderedListCombiner.intersectLists(iteratorIncident, iteratorAdjacent, Comparator.comparingInt(x -> x.v1), (x, y) -> new IdxPref(x.v1, (x.v2 + y.v2) / 2.0)).stream();
    }

    @Override
    default Stream<EdgeType> getMutualAdjacentTypes(int node)
    {
        Iterator<EdgeType> iteratorIncident = this.getIncidentTypes(node).iterator();
        Iterator<EdgeType> iteratorAdjacent = this.getAdjacentTypes(node).iterator();

        return OrderedListCombiner.intersectLists(iteratorIncident, iteratorAdjacent, Comparator.comparingInt(IdxValue::getIdx), (x, y) -> y).stream();
    }

    @Override
    default Stream<EdgeType> getMutualIncidentTypes(int node)
    {
        Iterator<EdgeType> iteratorIncident = this.getIncidentTypes(node).iterator();
        Iterator<EdgeType> iteratorAdjacent = this.getAdjacentTypes(node).iterator();
        return OrderedListCombiner.intersectLists(iteratorIncident, iteratorAdjacent, Comparator.comparingInt(IdxValue::getIdx), (x, y) -> x).stream();
    }

    @Override
    default Stream<EdgeType> getMutualTypes(int node)
    {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    default Stream<EdgeType> getNeighbourTypes(int node)
    {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    default Stream<IdxPref> getNeighbourWeights(int node)
    {
        throw new UnsupportedOperationException("Not supported");
    }
}
