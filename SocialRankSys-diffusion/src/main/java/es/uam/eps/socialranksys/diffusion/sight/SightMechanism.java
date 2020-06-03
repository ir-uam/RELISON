/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.socialranksys.diffusion.sight;

import es.uam.eps.socialranksys.diffusion.data.Data;
import es.uam.eps.socialranksys.diffusion.data.PropagatedInformation;
import es.uam.eps.socialranksys.diffusion.simulation.UserState;

import java.io.Serializable;

/**
 * Mechanism for deciding which information pieces are selected by the different users.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public interface SightMechanism<U extends Serializable,I extends Serializable,P>
{
    /*
     * Selects the information pieces that a user sees.
     * @param user The user that sees the data.
     * @param data The data.
     * @return A stream containing the pieces of information that will pass from new to seen
     */
  //  public Stream<PropagatedInformation> seeInformation(UserState<U> user, Data<U,I,P> data);
    
    /**
     * Resets the selections made.
     * @param data The data.
     */
    default void resetSelections(Data<U, I, P> data){}

    /**
     * Checks if a user sees or not a piece of information.
     * @param user the user.
     * @param data the full data.
     * @param prop the information piece received by a user.
     * @return true if the user sees the piece, false if it does not.
     */
    boolean seesInformation(UserState<U> user, Data<U, I, P> data, PropagatedInformation prop);
}
