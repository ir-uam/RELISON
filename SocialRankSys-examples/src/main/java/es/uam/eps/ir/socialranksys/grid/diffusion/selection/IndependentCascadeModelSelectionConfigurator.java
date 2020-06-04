/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.selection;

import es.uam.eps.socialranksys.diffusion.selections.IndependentCascadeModelSelectionMechanism;
import es.uam.eps.socialranksys.diffusion.selections.SelectionMechanism;

import java.io.Serializable;

/**
 * Configures a Independent Cascade Model selection mechanism.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public class IndependentCascadeModelSelectionConfigurator<U extends Serializable,I extends Serializable,P> implements SelectionConfigurator<U,I,P>
{
    /**
     * Identifier for the number of own pieces of information to propagate.
     */
    private final static String NUMOWN = "numOwn";
    /**
     * Identifier for the probability of propagating a received piece.
     */
    private final static String PROB = "prob";
    /**
     * Identifier for the number of propagated pieces of information to repropagate.
     */
    private final static String NUMREPR = "numRepr";
    
    @Override
    public SelectionMechanism<U,I,P> configure(SelectionParamReader params)
    {
        int numOwn = params.getParams().getIntegerValue(NUMOWN);
        double prob = params.getParams().getDoubleValue(PROB);
        
        if(params.getParams().getIntegerValues().containsKey(NUMREPR))
        {
            int numRepr = params.getParams().getIntegerValue(NUMREPR);
            return new IndependentCascadeModelSelectionMechanism<>(prob, numOwn, numRepr);
        }
        return new IndependentCascadeModelSelectionMechanism<>(prob, numOwn);
    }
    
}
