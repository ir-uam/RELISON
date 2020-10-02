/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph.multigraph;


import es.uam.eps.ir.socialranksys.graph.DirectedGraph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;

import java.util.stream.Stream;

/**
 * Interface for directed multi graphs.
 *
 * @param <U> type of the vertices
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface DirectedMultiGraph<U> extends MultiGraph<U>, DirectedGraph<U>
{
    @Override
    default Stream<Weights<U, Double>> getNeighbourhoodWeightsLists(U node, EdgeOrientation direction)
    {
        return switch (direction)
        {
            case OUT -> this.getAdjacentNodesWeightsLists(node);
            case IN -> this.getIncidentNodesWeightsLists(node);
            case UND -> this.getNeighbourNodesWeightsLists(node);
            default -> Stream.empty();
        };
    }

    @Override
    default Stream<Weights<U, Integer>> getNeighbourhoodTypesLists(U node, EdgeOrientation direction)
    {
        return switch (direction)
        {
            case OUT -> this.getAdjacentNodesTypesLists(node);
            case IN -> this.getIncidentNodesTypesLists(node);
            case UND -> this.getNeighbourNodesTypesLists(node);
            default -> Stream.empty();
        };
    }
}
