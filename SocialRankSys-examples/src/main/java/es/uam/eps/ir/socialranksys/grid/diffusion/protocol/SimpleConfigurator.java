package es.uam.eps.ir.socialranksys.grid.diffusion.protocol;

/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import es.uam.eps.ir.socialranksys.diffusion.protocols.Protocol;
import es.uam.eps.ir.socialranksys.diffusion.protocols.SimpleProtocol;

import java.io.Serializable;

/**
 * Configures a Simple protocol
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public class SimpleConfigurator<U extends Serializable,I extends Serializable,P> implements ProtocolConfigurator<U,I,P>
{
    /**
     * Identifier for the number of received pieces to propagate each iteration.
     */
    private final static String NUMREC = "numRec";
    /**
     * Identifier for the number of own pieces to propagate each iteration.
     */
    private final static String NUMOWN = "numOwn";

    @Override
    public Protocol<U,I,P> configure(ProtocolParamReader params)
    {
        int numRec = params.getParams().getIntegerValue(NUMREC);
        int numOwn = params.getParams().getIntegerValue(NUMOWN);
        
        return new SimpleProtocol<>(numOwn, numRec);
    }
}
