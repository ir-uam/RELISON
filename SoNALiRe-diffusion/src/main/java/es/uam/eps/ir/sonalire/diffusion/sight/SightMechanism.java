/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.diffusion.sight;

import es.uam.eps.ir.sonalire.diffusion.data.Data;
import es.uam.eps.ir.sonalire.diffusion.data.PropagatedInformation;
import es.uam.eps.ir.sonalire.diffusion.simulation.UserState;

import java.io.Serializable;
import java.util.List;

/**
 * Mechanism that decides which of the information pieces that a user has received are actually seen (payed attention to)
 * by each of the users in the network.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <P> type of the parameters.
 */
public interface SightMechanism<U extends Serializable,I extends Serializable,P>
{
    /**
     * For each user, this method preconfigures the sight mechanism (for example, selecting a fixed set of users whom each
     * user pays attention to).
     *
     * @param data the simulation data.
     */
    void resetSelections(Data<U, I, P> data);

    /**
     * Given a list of propagated information, it identifies which pieces the user has seen.
     * @param user  the current state of the user.
     * @param data  the simulation data.
     * @param prop  the list of propagated information.
     * @return a list containing all the information that the user pays attention to from the list of received pieces.
     */
    List<PropagatedInformation> seesInformation(UserState<U> user, Data<U,I,P> data, List<PropagatedInformation> prop);
}
