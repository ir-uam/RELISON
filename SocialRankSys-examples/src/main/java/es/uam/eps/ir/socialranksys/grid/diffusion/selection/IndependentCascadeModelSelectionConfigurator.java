/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.selection;

import es.uam.eps.ir.socialranksys.diffusion.selections.IndependentCascadeModelSelectionMechanism;
import es.uam.eps.ir.socialranksys.diffusion.selections.SelectionMechanism;
import es.uam.eps.ir.socialranksys.grid.Parameters;

import java.io.Serializable;

/**
 * Configures a selection mechanism that propagates a fixed number of own information pieces, and repropagates
 * pieces with a probability that only depends on the users receiving and propagating the information.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 *
 * @see IndependentCascadeModelSelectionMechanism
 */
public class IndependentCascadeModelSelectionConfigurator<U extends Serializable,I extends Serializable, F> implements SelectionConfigurator<U,I, F>
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
    public SelectionMechanism<U,I, F> configure(Parameters params)
    {
        int numOwn = params.getIntegerValue(NUMOWN);
        double prob = params.getDoubleValue(PROB);

        if(params.getIntegerValues().containsKey(NUMREPR))
        {
            int numRepr = params.getIntegerValue(NUMREPR);
            return new IndependentCascadeModelSelectionMechanism<>(prob, numOwn, numRepr);
        }
        return new IndependentCascadeModelSelectionMechanism<>(prob, numOwn);
    }
    
}
