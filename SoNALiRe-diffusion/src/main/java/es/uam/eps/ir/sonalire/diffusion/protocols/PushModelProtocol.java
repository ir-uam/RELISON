/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.diffusion.protocols;

import es.uam.eps.ir.sonalire.diffusion.expiration.AllNotPropagatedExpirationMechanism;
import es.uam.eps.ir.sonalire.diffusion.propagation.PushStrategyPropagationMechanism;
import es.uam.eps.ir.sonalire.diffusion.selections.CountSelectionMechanism;
import es.uam.eps.ir.sonalire.diffusion.sight.AllNotPropagatedSightMechanism;
import es.uam.eps.ir.sonalire.diffusion.update.NewestUpdateMechanism;
import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;

import java.io.Serializable;

/**
 * Model that applies the push strategy for diffunding the information. In this strategy, each
 * user selects a neighbour, and sends it all the information.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 *
 * <p>
 * <b>Reference:</b> A. Demers, D. Greene, C. Hauser, W. Irish, J. Larson. Epidemic algorithms for replicated database maintenance. ACM PODC 1987, pp. 1-12 (1987)
 * </p>
 */
public class PushModelProtocol<U extends Serializable,I extends Serializable, F> extends Protocol<U,I, F>
{
    /**
     * Constructor.
     * @param numOwn    number of own pieces of information to spread every iteration.
     * @param numRec    number of received pieces of information to spread every iteration.
     * @param numWait   number of steps before selecting again a certain neighbor.
     */
    public PushModelProtocol(int numOwn, int numRec, int numWait)
    {
        super(  new CountSelectionMechanism<>(numOwn, numRec),
                new AllNotPropagatedExpirationMechanism<>(),
                new NewestUpdateMechanism(),
                new PushStrategyPropagationMechanism<>(numWait, EdgeOrientation.IN),
                new AllNotPropagatedSightMechanism<>());
    }
    
}
