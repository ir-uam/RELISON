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
import es.uam.eps.ir.relison.diffusion.protocols.ProportionThresholdModelProtocol;

import java.io.Serializable;

/**
 * Configures a protocol in which propagates the received information if a large enough fraction of neighbors send the same piece to him/her.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 *
 * @see ProportionThresholdModelProtocol
 */
public class ProportionThresholdModelConfigurator<U extends Serializable,I extends Serializable, F> implements ProtocolConfigurator<U,I, F>
{
    /**
     * Identifier for the number of own pieces to propagate each iteration
     */
    private final static String NUMOWN = "numOwn";
    /**
     * Identifier for the number of pieces to repropagate each iteration
     */
    private final static String NUMREC = "numRec";
    /**
     * Identifier for the threshold to surpass for repropagating an information piece
     */
    private final static String THRESHOLD = "threshold";

    @Override
    public Protocol<U, I, F> configure(ProtocolParameterReader params)
    {
        int numOwn = params.getParams().getIntegerValue(NUMOWN);
        int numRec = params.getParams().getIntegerValue(NUMREC);
        double threshold = params.getParams().getDoubleValue(THRESHOLD);
        
        return new ProportionThresholdModelProtocol<>(numOwn, numRec, threshold);
    }
    
}
