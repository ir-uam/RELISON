/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph.multigraph.edges;

import java.util.stream.Stream;

/**
 * Class for the directed edges.
 * @author Javier Sanz-Cruzado Puig
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
    default Stream<MultiEdgeWeights> getIncidentWeight(int node)
    {
        return this.getNeighbourWeight(node);
    }
    
    @Override
    default Stream<MultiEdgeWeights> getAdjacentWeight(int node)
    {
        return this.getNeighbourWeight(node);
    }
    
    @Override
    default int getNeighbourCount(int node)
    {
        return this.getIncidentCount(node);
    }
}
