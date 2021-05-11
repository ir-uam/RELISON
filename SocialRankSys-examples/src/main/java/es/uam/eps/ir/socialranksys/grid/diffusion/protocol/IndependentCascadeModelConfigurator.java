/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.protocol;

import es.uam.eps.ir.socialranksys.diffusion.protocols.IndependentCascadeModelProtocol;
import es.uam.eps.ir.socialranksys.diffusion.protocols.Protocol;

import java.io.Serializable;

/**
 * Configures the independent cascade model protocol, where the repropagation of information depends only
 * on the users who receive and propagate such information.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 *
 * @see IndependentCascadeModelProtocol
 */
public class IndependentCascadeModelConfigurator<U extends Serializable,I extends Serializable, F> implements ProtocolConfigurator<U,I, F>
{
    /**
     * Identifier for the probability of propagating.
     */
    private final static String PROB = "prob";
    /**
     * Identifier for the number of own information pieces to propagate.
     */
    private final static String NUMOWN = "numOwn";

    @Override
    public Protocol<U,I, F> configure(YAMLProtocolParameterReader params)
    {
        double prob = params.getParams().getDoubleValue(PROB);
        int numOwn = params.getParams().getIntegerValue(NUMOWN);
        
        return new IndependentCascadeModelProtocol<>(prob, numOwn);
    }
}
