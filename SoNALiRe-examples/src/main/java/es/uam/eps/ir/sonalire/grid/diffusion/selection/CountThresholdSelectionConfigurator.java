/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.diffusion.selection;

import es.uam.eps.ir.sonalire.diffusion.selections.CountThresholdSelectionMechanism;
import es.uam.eps.ir.sonalire.diffusion.selections.SelectionMechanism;
import es.uam.eps.ir.sonalire.grid.Parameters;

import java.io.Serializable;

/**
 * Configures a selection mechanism that propagates a fixed number of own information pieces, and repropagates
 * pieces which have been received more than a fixed number of times.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 *
 * @see CountThresholdSelectionMechanism
 */
public class CountThresholdSelectionConfigurator<U extends Serializable,I extends Serializable, F> implements SelectionConfigurator<U,I, F>
{
    /**
     * Identifier for the number of own elements to propagate.
     */
    private final static String NUMOWN = "numOwn";
    /**
     * Identifier for the number of elements needed for repropagating an individual piece.
     */
    private final static String THRESHOLD = "threshold";
    /**
     * Identifier for the number of propagated pieces of information to repropagate.
     */
    private final static String NUMREPR = "numRepr";
    
    @Override
    public SelectionMechanism<U, I, F> configure(Parameters params)
    {
        int numOwn = params.getIntegerValue(NUMOWN);
        int threshold = params.getIntegerValue(THRESHOLD);
        
        if(params.getIntegerValues().containsKey(NUMREPR))
        {
            int numRepr = params.getIntegerValue(NUMREPR);
            return new CountThresholdSelectionMechanism<>(numOwn, threshold, numRepr);
        }
        else
        {
            return new CountThresholdSelectionMechanism<>(numOwn, threshold);
        }
    }
    
    
}
