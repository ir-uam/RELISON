/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.diffusion.expiration;

import es.uam.eps.ir.relison.diffusion.expiration.AllNotPropagatedExpirationMechanism;
import es.uam.eps.ir.relison.diffusion.expiration.ExpirationMechanism;
import es.uam.eps.ir.relison.diffusion.selections.LimitedCountThresholdSelectionMechanism;
import es.uam.eps.ir.relison.grid.Parameters;

import java.io.Serializable;

/**
 * Configures an expiration mechanism that discards all those received pieces which have not been propagated earlier.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 *
 * @see LimitedCountThresholdSelectionMechanism
 */
public class AllNotPropagatedExpirationConfigurator<U extends Serializable,I extends Serializable, F> implements ExpirationConfigurator<U,I, F>
{

    @Override
    public ExpirationMechanism<U, I, F> configure(Parameters params)
    {
        return new AllNotPropagatedExpirationMechanism<>();
    }
    
}
