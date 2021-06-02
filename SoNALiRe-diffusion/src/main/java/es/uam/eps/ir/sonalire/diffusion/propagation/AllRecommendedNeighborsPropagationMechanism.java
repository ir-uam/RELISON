/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.diffusion.propagation;

import es.uam.eps.ir.sonalire.diffusion.data.Data;
import es.uam.eps.ir.sonalire.diffusion.data.PropagatedInformation;
import es.uam.eps.ir.sonalire.diffusion.simulation.SimulationEdgeTypes;
import es.uam.eps.ir.sonalire.diffusion.simulation.UserState;
import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;

import java.io.Serializable;
import java.util.stream.Stream;

/**
 * Given a single piece of information, a user selects all the neighbors who can be reached
 * through a recommendation as the destination of the piece.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the items.
 * @param <P> type of the parameters.
 */
public class AllRecommendedNeighborsPropagationMechanism<U extends Serializable,I extends Serializable,P> implements PropagationMechanism<U,I,P>
{
    /**
     * The orientation for selecting the neighbors.
     */
    private final EdgeOrientation orientation;

    /**
     * Constructor.
     * @param orientation the orientation for selecting the neighbors.
     */
    public AllRecommendedNeighborsPropagationMechanism(EdgeOrientation orientation)
    {
        this.orientation = orientation;
    }

    @Override
    public Stream<U> getUsersToPropagate(PropagatedInformation information, UserState<U> originUser, Data<U, I,P> data)
    {
        U u = originUser.getUserId();
        // We select the whole set of neighbors.
        return data.getGraph().getNeighbourhood(u, orientation).filter(v ->
            {
                if(orientation.equals(EdgeOrientation.IN))
                {
                    return data.getGraph().getEdgeType(v, u) == SimulationEdgeTypes.RECOMMEND;
                }
                else if(orientation.equals(EdgeOrientation.OUT))
                {
                    return data.getGraph().getEdgeType(u, v) == SimulationEdgeTypes.RECOMMEND;
                }
                else
                {
                    return (data.getGraph().containsEdge(u, v) && data.getGraph().getEdgeType(u,v) == SimulationEdgeTypes.RECOMMEND) ||
                            (data.getGraph().containsEdge(v, u) && data.getGraph().getEdgeType(v,u) == SimulationEdgeTypes.RECOMMEND);
                }
            }
        );
    }

    @Override
    public boolean dependsOnInformationPiece() 
    {
        return false;
    }
    
}
