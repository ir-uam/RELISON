/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.protocol;

import es.uam.eps.ir.socialranksys.diffusion.protocols.CountThresholdModelProtocol;
import es.uam.eps.ir.socialranksys.diffusion.protocols.Protocol;

import java.io.Serializable;

/**
 * Configures a protocol in which propagates the received information if enough neighbors send the same piece to him/her.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 *
 * @see CountThresholdModelProtocol
 */
public class CountThresholdModelConfigurator<U extends Serializable,I extends Serializable, F> implements ProtocolConfigurator<U,I, F>
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
    public Protocol<U, I, F> configure(YAMLProtocolParameterReader params)
    {
        int numOwn = params.getParams().getIntegerValue(NUMOWN);
        int threshold = params.getParams().getIntegerValue(THRESHOLD);
        
        return new CountThresholdModelProtocol<>(numOwn, threshold);
    }
    
}
