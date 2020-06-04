/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.protocol;

import es.uam.eps.socialranksys.diffusion.protocols.Protocol;
import es.uam.eps.socialranksys.diffusion.protocols.PullModelProtocol;

import java.io.Serializable;

/**
 * Configures a Pull protocol
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public class PullModelConfigurator<U extends Serializable,I extends Serializable,P> implements ProtocolConfigurator<U,I,P>
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
    public Protocol<U, I, P> configure(ProtocolParamReader params)
    {
        int numRec = params.getParams().getIntegerValue(NUMREC);
        int numOwn = params.getParams().getIntegerValue(NUMOWN);
        int waitTime = params.getParams().getIntegerValue(WAITTIME);
        
        return new PullModelProtocol<>(numOwn, numRec, waitTime);
    }
}
