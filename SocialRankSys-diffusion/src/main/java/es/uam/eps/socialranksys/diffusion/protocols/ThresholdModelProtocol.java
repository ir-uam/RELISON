/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.socialranksys.diffusion.protocols;

import es.uam.eps.socialranksys.diffusion.expiration.AllNotPropagatedExpirationMechanism;
import es.uam.eps.socialranksys.diffusion.propagation.AllFollowersPropagationMechanism;
import es.uam.eps.socialranksys.diffusion.selections.ThresholdSelectionMechanism;
import es.uam.eps.socialranksys.diffusion.sight.AllSightMechanism;
import es.uam.eps.socialranksys.diffusion.update.OlderUpdateMechanism;

import java.io.Serializable;

/**
 * Threshold model protocol.
 * <br>
 * Main characteristics
 * <ul>
 *  <li><b>Selection mechanism:</b> Each user takes a fixed number of information pieces from his own list. In the case of the
 *  received list, each piece of information will be propagated depending on the proportion of the neighbors that has propagated
 *  the information to him. If this proportion exceeds a threshold, then, the information will be propagated.</li>
 *  <li><b>Expiration mechanism:</b> In this case, information pieces never expire: they will not be discarded.</li>
 *  <li><b>Update mechanism:</b> Since information pieces remain "in the memory of the users" once they are received, if a piece
 *  is received again, the list of users will be updated to contain the whole lot of users that have propagated the piece to the
 *  corresponding user</li>
 *  <li><b>Propagation mechanism:</b> In order to maximize the spreading, the information reaches all the followers of the user that
 *  propagates the information</li>
 *  <li><b>Sight mechanism:</b> All users see all the received information pieces.</li>
 * </ul>
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public class ThresholdModelProtocol<U extends Serializable,I extends Serializable,P> extends Protocol<U,I,P>
{
    /**
     * Constructor.
     * @param numOwn Number of own pieces to propagate each iteration.
     * @param numRec Number of pieces to repropagate.
     * @param threshold Threshold that has to be surpassed.
     */
    public ThresholdModelProtocol(int numOwn, int numRec, double threshold) 
    {
        super(new ThresholdSelectionMechanism<>(numOwn, numRec, threshold),
              new AllNotPropagatedExpirationMechanism<>(),
              new OlderUpdateMechanism(),
              new AllFollowersPropagationMechanism<>(),
              new AllSightMechanism<>());
    }
    
}
