/* 
 *  Copyright (C) 2015 Information Retrieval Group at Universidad AutÃ³noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.graph.multigraph.edges;

import java.util.stream.Stream;

/**
 * Class for the directed edges.
 * @author Javier Sanz-Cruzado Puig
 */
public interface DirectedMultiEdges extends MultiEdges
{   
    @Override
    default Stream<Integer> getNeighbourNodes(int node)
    {
        return Stream.concat(this.getIncidentNodes(node), this.getAdjacentNodes(node)).distinct();
    }
    
    @Override
    default Stream<MultiEdgeTypes> getNeighbourTypes(int node)
    {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    default Stream<MultiEdgeWeights> getNeighbourWeight(int node)
    {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    default int getNeighbourCount(int node)
    {
        return this.getAdjacentCount(node) + this.getIncidentCount(node);
    }

}
