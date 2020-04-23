/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph.edges;

import es.uam.eps.ir.ranksys.fast.preference.IdxPref;

import java.util.stream.Stream;

/**
 * Interface for the directed edges.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface UndirectedEdges extends Edges
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
    default Stream<Integer> getMutualNodes(int node)
    {
        return this.getNeighbourNodes(node);
    }

    @Override
    default Stream<EdgeType> getIncidentTypes(int node)
    {
        return this.getNeighbourTypes(node);
    }

    @Override
    default Stream<EdgeType> getAdjacentTypes(int node)
    {
        return this.getNeighbourTypes(node);
    }

    @Override
    default Stream<IdxPref> getIncidentWeights(int node)
    {
        return this.getNeighbourWeights(node);
    }

    @Override
    default Stream<EdgeType> getMutualAdjacentTypes(int node)
    {
        return this.getNeighbourTypes(node);
    }

    @Override
    default Stream<EdgeType> getMutualIncidentTypes(int node)
    {
        return this.getNeighbourTypes(node);
    }

    @Override
    default Stream<EdgeType> getMutualTypes(int node)
    {
        return this.getNeighbourTypes(node);
    }

    @Override
    default Stream<IdxPref> getAdjacentWeights(int node)
    {
        return this.getNeighbourWeights(node);
    }

    @Override
    default Stream<IdxPref> getMutualAdjacentWeights(int node)
    {
        return this.getNeighbourWeights(node);
    }

    @Override
    default Stream<IdxPref> getMutualIncidentWeights(int node)
    {
        return this.getNeighbourWeights(node);
    }

    @Override
    default Stream<IdxPref> getMutualWeights(int node)
    {
        return this.getNeighbourWeights(node);
    }

    @Override
    default long getAdjacentCount(int node)
    {
        return this.getNeighbourCount(node);
    }

    @Override
    default long getMutualCount(int node)
    {
        return this.getNeighbourCount(node);
    }

}
