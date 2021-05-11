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
 * Configures a temporal protocol, which replicates an old diffusion process.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 *
 * @see TemporalProtocol
 */
public class TemporalConfigurator<U extends Serializable,I extends Serializable, F> implements ProtocolConfigurator<U,I, F>
{
    /**
     * Identifier for the parameter that indicates if received pieces can be propagated after the true timestamp moment (false) or not (true).
     */
    private final static String PURE = "pure";

    @Override
    public Protocol<U, I, F> configure(YAMLProtocolParameterReader params)
    {
        boolean param = params.getParams().getBooleanValue(PURE);
        return new TemporalProtocol<>(param);
    }
    
}
