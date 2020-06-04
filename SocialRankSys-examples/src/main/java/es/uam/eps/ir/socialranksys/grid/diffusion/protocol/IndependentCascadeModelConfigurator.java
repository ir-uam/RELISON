/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.protocol;

import es.uam.eps.socialranksys.diffusion.protocols.IndependentCascadeModelProtocol;
import es.uam.eps.socialranksys.diffusion.protocols.Protocol;

import java.io.Serializable;

/**
 * Configures an Independent Cascade Model protocol
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public class IndependentCascadeModelConfigurator<U extends Serializable,I extends Serializable,P> implements ProtocolConfigurator<U,I,P>
{
    /**
     * Identifier for the probability
     */
    private final static String PROB = "prob";
    /**
     * Identifier for the number of own information pieces to propagate.
     */
    private final static String NUMOWN = "numOwn";
    

    @Override
    public Protocol<U,I,P> configure(ProtocolParamReader params)
    {
        double prob = params.getParams().getDoubleValue(PROB);
        int numOwn = params.getParams().getIntegerValue(NUMOWN);
        
        return new IndependentCascadeModelProtocol<>(prob, numOwn);
    }
}
