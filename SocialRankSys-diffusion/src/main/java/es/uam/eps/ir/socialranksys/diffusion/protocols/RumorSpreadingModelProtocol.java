/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.protocols;

import es.uam.eps.ir.socialranksys.diffusion.expiration.AllNotPropagatedExpirationMechanism;
import es.uam.eps.ir.socialranksys.diffusion.propagation.PullPushStrategyPropagationMechanism;
import es.uam.eps.ir.socialranksys.diffusion.selections.CountSelectionMechanism;
import es.uam.eps.ir.socialranksys.diffusion.sight.AllSightMechanism;
import es.uam.eps.ir.socialranksys.diffusion.update.IndependentCascadeModelUpdateMechanism;

import java.io.Serializable;

/**
 * Adaptation of the pull-push protocol.
 *
 * <p><b>Reference:</b> B. Doerr, M. Fouz, T. Friedrich, Social networks spread rumors in sublogarithmic time, 43rd Annual ACM Symposium on Theory of Computing (STOC 2011), pp. 21-30. (2011)</p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public class RumorSpreadingModelProtocol<U extends Serializable,I extends Serializable,P> extends Protocol<U,I,P>
{

    /**
     * Constructor.
     * @param numOwn Number of own pieces of information to spread every iteration.
     * @param numRec Number of received pieces of information to spread every iteration.
     * @param waitTime Number of iterations before a user can be revisited.
     */
    public RumorSpreadingModelProtocol(int numOwn, int numRec, int waitTime)
    {
        super(  new CountSelectionMechanism<>(numOwn, numRec),
                new AllNotPropagatedExpirationMechanism<>(),
                new IndependentCascadeModelUpdateMechanism(),
                new PullPushStrategyPropagationMechanism<>(waitTime),
                new AllSightMechanism<>());
    }
    
}
