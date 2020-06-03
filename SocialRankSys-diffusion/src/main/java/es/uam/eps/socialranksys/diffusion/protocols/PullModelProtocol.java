/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.socialranksys.diffusion.protocols;

import es.uam.eps.socialranksys.diffusion.expiration.AllNotPropagatedExpirationMechanism;
import es.uam.eps.socialranksys.diffusion.propagation.PullStrategyPropagationMechanism;
import es.uam.eps.socialranksys.diffusion.selections.CountSelectionMechanism;
import es.uam.eps.socialranksys.diffusion.sight.AllSightMechanism;
import es.uam.eps.socialranksys.diffusion.update.IndependentCascadeModelUpdateMechanism;

import java.io.Serializable;

/**
 * Protocol that applies the push strategy for diffunding the information. In this strategy, each
 * user selects a neighbour, and obtains from it all the information.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <P> type of the parameters.
 */
public class PullModelProtocol<U extends Serializable,I extends Serializable,P> extends Protocol<U,I,P>
{
    /**
     * Constructor.
     * @param numOwn Number of own information to propagate.
     * @param numRec Number of received information to propagate.
     * @param numWait Number of steps before selecting an already visited user.
     */
    public PullModelProtocol(int numOwn, int numRec, int numWait)
    {
        super(  new CountSelectionMechanism<>(numOwn, numRec),
                new AllNotPropagatedExpirationMechanism<>(),
                new IndependentCascadeModelUpdateMechanism(),
                new PullStrategyPropagationMechanism<>(numWait),
                new AllSightMechanism<>());
    }
    
}
