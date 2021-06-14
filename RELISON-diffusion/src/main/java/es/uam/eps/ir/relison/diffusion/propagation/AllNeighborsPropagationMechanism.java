/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.propagation;

import es.uam.eps.ir.relison.diffusion.data.Data;
import es.uam.eps.ir.relison.diffusion.data.PropagatedInformation;
import es.uam.eps.ir.relison.diffusion.simulation.UserState;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;

import java.io.Serializable;
import java.util.stream.Stream;

/**
 * Given a single piece of information, a user selects all his/her neighbors as the destination of the piece.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the items.
 * @param <P> type of the parameters.
 */
public class AllNeighborsPropagationMechanism<U extends Serializable,I extends Serializable,P> implements PropagationMechanism<U,I,P>
{
    /**
     * The orientation for selecting the neighbors.
     */
    private final EdgeOrientation orientation;

    /**
     * Constructor.
     * @param orientation the orientation for selecting the neighbors.
     */
    public AllNeighborsPropagationMechanism(EdgeOrientation orientation)
    {
        this.orientation = orientation;
    }

    @Override
    public Stream<U> getUsersToPropagate(PropagatedInformation information, UserState<U> originUser, Data<U, I,P> data)
    {
        // We select the whole set of neighbors.
        return data.getGraph().getNeighbourhood(originUser.getUserId(), orientation);
    }

    @Override
    public boolean dependsOnInformationPiece() 
    {
        return false;
    }
    
}
