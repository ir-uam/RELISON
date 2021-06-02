/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.diffusion.protocols;

import es.uam.eps.ir.sonalire.diffusion.expiration.ExpirationMechanism;
import es.uam.eps.ir.sonalire.diffusion.propagation.PropagationMechanism;
import es.uam.eps.ir.sonalire.diffusion.selections.SelectionMechanism;
import es.uam.eps.ir.sonalire.diffusion.sight.SightMechanism;
import es.uam.eps.ir.sonalire.diffusion.update.UpdateMechanism;

import java.io.Serializable;

/**
 * Information propagation protocol.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 */
public abstract class Protocol<U extends Serializable,I extends Serializable, F>
{
    /**
     * Mechanism for selecting the information the user propagates.
     */
    private final SelectionMechanism<U,I, F> selection;
    /**
     * Mechanism for discarding information pieces over time.
     */
    private final ExpirationMechanism<U,I, F> expiration;
    /**
     * Mechanism for updating the list of information to propagate.
     */
    private final UpdateMechanism update;
    /**
     * Mechanism for selecting the users we want to propagate the information.
     */
    private final PropagationMechanism<U,I, F> prop;
    /**
     * Mechanism for selecting which information users actually see
     */
    private final SightMechanism<U,I, F> sight;

    /**
     * Constructor.
     * @param selection     mechanism for selecting the information the user propagates.
     * @param expiration    mechanism for discarding information pieces over time.
     * @param update        mechanism for updating the list of information to propagate.
     * @param prop          mechanism for selecting the users towards whom propagate the information.
     * @param sight         mechanism for selecting the information pieces that a user sees.
     */
    public Protocol(SelectionMechanism<U, I, F> selection, ExpirationMechanism<U, I, F> expiration, UpdateMechanism update, PropagationMechanism<U, I, F> prop, SightMechanism<U, I, F> sight) {
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
    public SelectionMechanism<U, I, F> getSelection()
    {
        return selection;
    }

    /**
     * Obtains the expiration mechanism.
     * @return the expiration mechanism.
     */
    public ExpirationMechanism<U, I, F> getExpiration()
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
    public PropagationMechanism<U, I, F> getProp()
    {
        return prop;
    }

    /**
     * Obtains the sight mechanism.
     * @return the sight mechanism.
     */
    public SightMechanism<U, I, F> getSight()
    {
        return sight;
    }
    
    
}
