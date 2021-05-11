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
import es.uam.eps.ir.socialranksys.diffusion.propagation.AllNeighborsPropagationMechanism;
import es.uam.eps.ir.socialranksys.diffusion.selections.CountSelectionMechanism;
import es.uam.eps.ir.socialranksys.diffusion.sight.AllNotPropagatedSightMechanism;
import es.uam.eps.ir.socialranksys.diffusion.update.NewestUpdateMechanism;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;

import java.io.Serializable;

/**
 * Simple simulator. Each iteration, propagates a certain number of own information, and a certain number
 * of received information.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 */
public class SimpleProtocol<U extends Serializable,I extends Serializable, F> extends Protocol<U,I, F>
{
    /**
     * Constructor.
     * @param numOwn number of own information pieces to propagate each iteration.
     * @param numRec number of received information pieces to propagate each iteration.
     */
    public SimpleProtocol(int numOwn, int numRec)
    {
        super(  new CountSelectionMechanism<>(numOwn, numRec),
                new AllNotPropagatedExpirationMechanism<>(),
                new NewestUpdateMechanism(),
                new AllNeighborsPropagationMechanism<>(EdgeOrientation.IN),
                new AllNotPropagatedSightMechanism<>());
    }
    
}
