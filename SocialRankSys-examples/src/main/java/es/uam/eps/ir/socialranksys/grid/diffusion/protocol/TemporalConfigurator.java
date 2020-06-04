/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.protocol;

import es.uam.eps.ir.socialranksys.diffusion.protocols.Protocol;
import es.uam.eps.ir.socialranksys.diffusion.protocols.TemporalProtocol;

import java.io.Serializable;

/**
 * Configurator for a Temporal protocol.
 * @author Javier Sanz-Cruzado Puig
 * 
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the features.
 *
 * */
public class TemporalConfigurator<U extends Serializable,I extends Serializable,P> implements ProtocolConfigurator<U,I,P> 
{
    /**
     * Identifier for the parameter that indicates if received pieces can be propagated after the true timestamp moment (false) or not (true).
     */
    private final static String PURE = "pure";

    @Override
    public Protocol<U, I, P> configure(ProtocolParamReader params)
    {
        boolean param = params.getParams().getBooleanValue(PURE);
        return new TemporalProtocol<>(param);
    }
    
}
