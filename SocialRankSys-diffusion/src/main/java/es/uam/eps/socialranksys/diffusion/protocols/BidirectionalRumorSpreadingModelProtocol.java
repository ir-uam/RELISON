/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.socialranksys.diffusion.protocols;

import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.socialranksys.diffusion.expiration.AllNotPropagatedExpirationMechanism;
import es.uam.eps.socialranksys.diffusion.propagation.PullPushStrategyPropagationMechanism;
import es.uam.eps.socialranksys.diffusion.selections.CountSelectionMechanism;
import es.uam.eps.socialranksys.diffusion.sight.AllSightMechanism;
import es.uam.eps.socialranksys.diffusion.update.IndependentCascadeModelUpdateMechanism;

import java.io.Serializable;

/**
 * Adaptation of the pull-push protocol.
 * 
 * Doerr, B., Fouz, M., Friedrich, T., Social networks spread rumors in sublogarithmic time, 43rd Annual ACM Symposium on Theory of Computing (STOC 2011), June 2011, pp. 21-30.
 * 
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public class BidirectionalRumorSpreadingModelProtocol<U extends Serializable,I extends Serializable,P> extends Protocol<U,I,P>
{

    /**
     * Constructor.
     * @param numOwn Number of own pieces of information to spread every iteration.
     * @param numRec Number of received pieces of information to spread every iteration.
     * @param waitTime Number of iterations before a user can be revisited.
     */
    public BidirectionalRumorSpreadingModelProtocol(int numOwn, int numRec, int waitTime)
    {
        super(  new CountSelectionMechanism<>(numOwn, numRec),
                new AllNotPropagatedExpirationMechanism<>(),
                new IndependentCascadeModelUpdateMechanism(),
                new PullPushStrategyPropagationMechanism<>(waitTime, EdgeOrientation.UND),
                new AllSightMechanism<>());
    }
    
}
