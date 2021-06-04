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

import java.io.Serializable;
import java.util.stream.Stream;

/**
 * Mechanism for selecting the set of users towards whom each user in the network propagates his/her information
 * pieces.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the items.
 * @param <P> type of the parameters.
 */
public interface PropagationMechanism<U extends Serializable,I extends Serializable,P>
{
    /**
     * Selects the users to which propagate a single piece of information.
     * @param information   the information piece to propagate.
     * @param originUser    the user who propagates the information piece.
     * @param data          the complete data for the simulation.
     * @return the stream of users to which propagate the single piece of information.
     */
    Stream<U> getUsersToPropagate(PropagatedInformation information, UserState<U> originUser, Data<U, I, P> data);

    /**
     * It resets the selections that this mechanism did in past iterations.
     * @param data the data.
     */
    default void resetSelections(Data<U, I, P> data){}
    
    /**
     * This indicates whether the selection of the users depends or not on the information piece which we want to
     * propagate.
     * @return true if it depends on the information piece, false if it does not.
     */
    boolean dependsOnInformationPiece();
    
}
