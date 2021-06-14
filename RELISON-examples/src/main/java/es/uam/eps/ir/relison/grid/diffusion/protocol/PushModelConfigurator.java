/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.diffusion.protocol;

import es.uam.eps.ir.relison.diffusion.protocols.Protocol;
import es.uam.eps.ir.relison.diffusion.protocols.PushModelProtocol;

import java.io.Serializable;

/**
 * Configures the push model protocol.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 *
 * @see PushModelProtocol
 */
public class PushModelConfigurator<U extends Serializable,I extends Serializable, F> implements ProtocolConfigurator<U,I, F>
{
    /**
     * Identifier for the number of received information pieces to propagate each iteration.
     */
    private final static String NUMREC = "numRec";
    /**
     * Identifier for the number of own information pieces to propagate each iteration.
     */
    private final static String NUMOWN = "numOwn";
    /**
     * Identifier for the minimum time between the same neighborhood selections.
     */
    private final static String WAITTIME = "waitTime";
   
    @Override
    public Protocol<U, I, F> configure(ProtocolParameterReader params)
    {
        int numRec = params.getParams().getIntegerValue(NUMREC);
        int numOwn = params.getParams().getIntegerValue(NUMOWN);
        int waitTime = params.getParams().getIntegerValue(WAITTIME);
        
        return new PushModelProtocol<>(numOwn, numRec, waitTime);
    }
}
