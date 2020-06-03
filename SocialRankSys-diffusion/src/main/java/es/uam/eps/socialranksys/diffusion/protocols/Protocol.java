/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.socialranksys.diffusion.protocols;

import es.uam.eps.socialranksys.diffusion.expiration.ExpirationMechanism;
import es.uam.eps.socialranksys.diffusion.propagation.PropagationMechanism;
import es.uam.eps.socialranksys.diffusion.selections.SelectionMechanism;
import es.uam.eps.socialranksys.diffusion.sight.SightMechanism;
import es.uam.eps.socialranksys.diffusion.update.UpdateMechanism;

import java.io.Serializable;

/**
 * Information propagation protocol.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the items.
 * @param <P> Type of the parameters.
 */
public abstract class Protocol<U extends Serializable,I extends Serializable,P> 
{
    /**
     * Mechanism for selecting the information the user propagates.
     */
    private final SelectionMechanism<U,I,P> selection;
    /**
     * Mechanism for discarding information pieces over time.
     */
    private final ExpirationMechanism<U,I,P> expiration;
    /**
     * Mechanism for updating the list of information to propagate.
     */
    private final UpdateMechanism update;
    /**
     * Mechanism for selecting the users we want to propagate the information.
     */
    private final PropagationMechanism<U,I,P> prop;
    /**
     * Mechanism for selecting which information users actually see
     */
    private final SightMechanism<U,I,P> sight;

    /**
     * Constructor.
     * @param selection Mechanism for selecting the information the user propagates.
     * @param expiration Mechanism for discarding information pieces over time.
     * @param update Mechanism for updating the list of information to propagate.
     * @param prop Propagation mechanism
     * @param sight sight mechanism
     */
    public Protocol(SelectionMechanism<U, I, P> selection, ExpirationMechanism<U, I, P> expiration, UpdateMechanism update, PropagationMechanism<U, I, P> prop, SightMechanism<U, I, P> sight) {
        this.selection = selection;
        this.expiration = expiration;
        this.update = update;
        this.prop = prop;
        this.sight = sight;
    }

    /**
     * Obtains the selection mechanism.
     * @return the selection mechanism.
     */
    public SelectionMechanism<U, I, P> getSelection() 
    {
        return selection;
    }

    /**
     * Obtains the expiration mechanism.
     * @return the expiration mechanism.
     */
    public ExpirationMechanism<U, I, P> getExpiration() 
    {
        return expiration;
    }

    /**
     * Obtains the update mechanism.
     * @return the update mechanism.
     */
    public UpdateMechanism getUpdate() 
    {
        return update;
    }

    /**
     * Obtains the propagation mechanism.
     * @return the propagation mechanism.
     */
    public PropagationMechanism<U, I, P> getProp() 
    {
        return prop;
    }

    /**
     * Obtains the sight mechanism.
     * @return the sight mechanism.
     */
    public SightMechanism<U, I, P> getSight() 
    {
        return sight;
    }
    
    
}
