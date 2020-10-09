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
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public class CustomProtocol<U extends Serializable,I extends Serializable,P> extends Protocol<U,I,P> 
{

    /**
     * Constructor.
     * @param selection Mechanism for selecting the information the user propagates.
     * @param expiration Mechanism for discarding information pieces over time.
     * @param update Mechanism for updating the list of information to propagate.
     * @param prop Propagation mechanism
     * @param sight sight mechanism
     */
    public CustomProtocol(SelectionMechanism<U, I, P> selection, ExpirationMechanism<U, I, P> expiration, UpdateMechanism update, PropagationMechanism<U, I, P> prop, SightMechanism<U, I, P> sight)
    {
        super(selection, expiration, update, prop, sight);
    }
    
}
