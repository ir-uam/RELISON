/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.propagation;

import es.uam.eps.ir.socialranksys.diffusion.data.Data;
import es.uam.eps.ir.socialranksys.diffusion.data.PropagatedInformation;
import es.uam.eps.ir.socialranksys.diffusion.simulation.UserState;

import java.io.Serializable;
import java.util.stream.Stream;

/**
 * Mechanism for selecting the users to which propagate the different pieces of information to propagate.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> Type of the users.
 * @param <I> Type of the items.
 * @param <P> Type of the parameters.
 */
public interface PropagationMechanism<U extends Serializable,I extends Serializable,P>
{
    /**
     * Selects the users to which propagate a single piece of information.
     * @param information The propagated information.
     * @param originUser the user which propagates the information.
     * @param data the full data.
     * @return the stream of users to which propagate a single piece of information.
     */
    Stream<U> getUsersToPropagate(PropagatedInformation information, UserState<U> originUser, Data<U, I, P> data);

    /**
     * Resets the selections made.
     * @param data The data.
     */
    default void resetSelections(Data<U, I, P> data){}
    
    /**
     * Indicates if the selection of users depends or not on which information piece is about to be propagated.
     * @return true if it depends on the information piece, false if it does not.
     */
    boolean dependsOnInformationPiece();
    
}
