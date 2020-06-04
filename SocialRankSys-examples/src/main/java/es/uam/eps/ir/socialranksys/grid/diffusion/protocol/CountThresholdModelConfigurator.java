/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.protocol;

import es.uam.eps.socialranksys.diffusion.protocols.CountThresholdModelProtocol;
import es.uam.eps.socialranksys.diffusion.protocols.Protocol;

import java.io.Serializable;

/**
 * Configures a Count Threshold model protocol
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public class CountThresholdModelConfigurator<U extends Serializable,I extends Serializable,P> implements ProtocolConfigurator<U,I,P> 
{
    /**
     * Identifier for the number of own pieces to propagate each iteration
     */
    private final static String NUMOWN = "numOwn";
    /**
     * Identifier for the threshold to surpass for repropagating an information piece
     */
    private final static String THRESHOLD = "threshold";

    @Override
    public Protocol<U, I, P> configure(ProtocolParamReader params)
    {
        int numOwn = params.getParams().getIntegerValue(NUMOWN);
        int threshold = params.getParams().getIntegerValue(THRESHOLD);
        
        return new CountThresholdModelProtocol<>(numOwn, threshold);
    }
    
}
