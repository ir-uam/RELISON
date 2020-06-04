/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.protocols;

import es.uam.eps.ir.socialranksys.diffusion.expiration.AllNotRealPropagatedTimestampExpirationMechanism;
import es.uam.eps.ir.socialranksys.diffusion.propagation.AllFollowersPropagationMechanism;
import es.uam.eps.ir.socialranksys.diffusion.selections.LooseTimestampBasedSelectionMechanism;
import es.uam.eps.ir.socialranksys.diffusion.selections.PureTimestampBasedSelectionMechanism;
import es.uam.eps.ir.socialranksys.diffusion.sight.AllNotDiscardedSightMechanism;
import es.uam.eps.ir.socialranksys.diffusion.update.IndependentCascadeModelUpdateMechanism;

import java.io.Serializable;

/**
 * Simulation protocol that considers the timestamps. This protocol aims at showing the true propagation
 * of information through the network: it only propagates new information when the simulation iteration
 * represents the timestamp of creation of the information piece, and a user propagates contents from
 * other users only if they had been previously propagated in real life.
 *
 * @author Javier Sanz-Cruzado Puig
 *
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the features.
 */
public class TemporalProtocol<U extends Serializable, I extends Serializable, P> extends Protocol<U,I,P>
{
    /**
     * Constructor.
     * @param pure true if information received from other users cannot be repropagated after the timestamp
     * of the repropagation has been visited, false if it can.
     */
    public TemporalProtocol(boolean pure)
    {
        super ((pure) ? new PureTimestampBasedSelectionMechanism<>() : new LooseTimestampBasedSelectionMechanism<>(),
               new AllNotRealPropagatedTimestampExpirationMechanism<>(),
               new IndependentCascadeModelUpdateMechanism(),
               new AllFollowersPropagationMechanism<>(),
               new AllNotDiscardedSightMechanism<>());
    }
}
