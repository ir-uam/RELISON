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
import es.uam.eps.ir.socialranksys.diffusion.sight.AllNotPropagatedSightMechanism;
import es.uam.eps.ir.socialranksys.diffusion.update.NewestUpdateMechanism;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;

import java.io.Serializable;

/**
 * Adaptation of the pull-push protocol.
 *
 * <p><b>Reference:</b> B. Doerr, M. Fouz, T. Friedrich, Social networks spread rumors in sublogarithmic time, 43rd Annual ACM Symposium on Theory of Computing (STOC 2011), pp. 21-30. (2011)</p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 */
public class BidirectionalRumorSpreadingModelProtocol<U extends Serializable,I extends Serializable, F> extends Protocol<U,I, F>
{

    /**
     * Constructor.
     * @param numOwn    number of own pieces of information to spread every iteration.
     * @param numRec    number of received pieces of information to spread every iteration.
     * @param waitTime  number of iterations before a user can be revisited.
     */
    public BidirectionalRumorSpreadingModelProtocol(int numOwn, int numRec, int waitTime)
    {
        super(  new CountSelectionMechanism<>(numOwn, numRec),
                new AllNotPropagatedExpirationMechanism<>(),
                new NewestUpdateMechanism(),
                new PullPushStrategyPropagationMechanism<>(waitTime, EdgeOrientation.UND),
                new AllNotPropagatedSightMechanism<>());
    }
    
}
