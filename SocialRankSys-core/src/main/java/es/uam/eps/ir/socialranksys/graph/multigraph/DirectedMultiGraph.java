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
 * @author Javier Sanz-Cruzado Puig
 * @param <V> type of the vertices
 */
public interface DirectedMultiGraph<V> extends MultiGraph<V>, DirectedGraph<V>
{    
    @Override
    default Stream<Weights<V,Double>> getNeighbourhoodWeightsLists(V node, EdgeOrientation direction)
    {
        switch(direction)
        {
            case OUT:
                return this.getAdjacentNodesWeightsLists(node);
            case IN:
                return this.getIncidentNodesWeightsLists(node);
            case UND:
                return this.getNeighbourNodesWeightsLists(node);
            default:
                return Stream.empty();
        }
    }
    @Override
    default Stream<Weights<V,Integer>> getNeighbourhoodTypesLists(V node, EdgeOrientation direction)
    {
        switch(direction)
        {
            case OUT:
                return this.getAdjacentNodesTypesLists(node);
            case IN:
                return this.getIncidentNodesTypesLists(node);
            case UND:
                return this.getNeighbourNodesTypesLists(node);
            default:
                return Stream.empty();
        }
    }    
}
