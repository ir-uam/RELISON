/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.protocols;

import es.uam.eps.ir.socialranksys.diffusion.expiration.ExpirationMechanism;
import es.uam.eps.ir.socialranksys.diffusion.propagation.PropagationMechanism;
import es.uam.eps.ir.socialranksys.diffusion.selections.SelectionMechanism;
import es.uam.eps.ir.socialranksys.diffusion.sight.SightMechanism;
import es.uam.eps.ir.socialranksys.diffusion.update.UpdateMechanism;

import java.io.Serializable;

/**
 * Class for building custom protocols.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 */
public class CustomProtocol<U extends Serializable,I extends Serializable, F> extends Protocol<U,I, F>
{
    /**
     * Constructor.
     * @param selection     mechanism for selecting the information the user propagates.
     * @param expiration    mechanism for discarding information pieces over time.
     * @param update        mechanism for updating the list of information to propagate.
     * @param prop          mechanism for selecting the users towards whom propagate the information.
     * @param sight         mechanism for selecting the information pieces that a user sees.
     */
    public CustomProtocol(SelectionMechanism<U, I, F> selection, ExpirationMechanism<U, I, F> expiration, UpdateMechanism update, PropagationMechanism<U, I, F> prop, SightMechanism<U, I, F> sight)
    {
        super(selection, expiration, update, prop, sight);
    }
    
}
