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
 * Interface for unweighted edges.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface UnweightedEdges extends Edges
{
    @Override
    default double getEdgeWeight(int orig, int dest)
    {
        if (this.containsEdge(orig, dest))
        {
            return EdgeWeight.getDefaultValue();
        }
        else
        {
            return EdgeWeight.getErrorValue();
        }
    }

    @Override
    default Stream<IdxPref> getIncidentWeights(int node)
    {
        return this.getIncidentNodes(node).map(val -> new EdgeWeight(val, EdgeWeight.getDefaultValue()));
    }

    @Override
    default Stream<IdxPref> getAdjacentWeights(int node)
    {
        return this.getAdjacentNodes(node).map(val -> new EdgeWeight(val, EdgeWeight.getDefaultValue()));
    }

    @Override
    default Stream<IdxPref> getNeighbourWeights(int node)
    {
        return this.getNeighbourNodes(node).map(val -> new EdgeWeight(val, EdgeWeight.getDefaultValue()));
    }

}
