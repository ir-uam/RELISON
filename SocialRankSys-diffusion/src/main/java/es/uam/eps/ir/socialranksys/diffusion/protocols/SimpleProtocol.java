/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.protocols;

import es.uam.eps.ir.socialranksys.diffusion.expiration.AllNotPropagatedExpirationMechanism;
import es.uam.eps.ir.socialranksys.diffusion.propagation.AllFollowersPropagationMechanism;
import es.uam.eps.ir.socialranksys.diffusion.selections.CountSelectionMechanism;
import es.uam.eps.ir.socialranksys.diffusion.sight.AllSightMechanism;
import es.uam.eps.ir.socialranksys.diffusion.update.IndependentCascadeModelUpdateMechanism;

import java.io.Serializable;

/**
 * Simple simulator. Each iteration, propagates a certain number of own information, and a certain number
 * of received information.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the items.
 * @param <P> Type of the parameters.
 */
public class SimpleProtocol<U extends Serializable,I extends Serializable,P> extends Protocol<U,I,P>
{
    /**
     * Constructor.
     * @param numOwn Number of own information pieces to propagate each iteration.
     * @param numRec Number of received information pieces to propagate each iteration.
     */
    public SimpleProtocol(int numOwn, int numRec)
    {
        super(  new CountSelectionMechanism<>(numOwn, numRec),
                new AllNotPropagatedExpirationMechanism<>(),
                new IndependentCascadeModelUpdateMechanism(),
                new AllFollowersPropagationMechanism<>(),
                new AllSightMechanism<>());
    }
    
}
