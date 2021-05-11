/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.selection;

import es.uam.eps.ir.socialranksys.diffusion.selections.CountRealPropagatedSelectionMechanism;
import es.uam.eps.ir.socialranksys.diffusion.selections.SelectionMechanism;
import es.uam.eps.ir.socialranksys.grid.Parameters;

import java.io.Serializable;

/**
 * Configures a selection mechanism that propagates a fixed number of own information pieces, and repropagate
 * some of the received pieces which the user did repropagate in a real diffusion process.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 *
 * @see CountRealPropagatedSelectionMechanism
 */
public class CountRealPropagatedSelectionConfigurator<U extends Serializable,I extends Serializable, F> implements SelectionConfigurator<U,I, F>
{
    /**
     * Identifier for the number of own pieces of information to propagate.
     */
    private final static String NUMOWN = "numOwn";
    /**
     * Identifier for the number of received pieces to propagate.
     */
    private final static String NUMREC = "numRec";
    /**
     * Identifier for the number of propagated pieces of information to repropagate.
     */
    private final static String NUMREPR = "numRepr";
    
    @Override
    public SelectionMechanism<U,I, F> configure(Parameters params)
    {
        int numOwn = params.getIntegerValue(NUMOWN);
        int numRec = params.getIntegerValue(NUMREC);
        if(params.getIntegerValue(NUMREPR) != null)
        {
            int numRepr = params.getIntegerValue(NUMREPR);
            return new CountRealPropagatedSelectionMechanism<>(numOwn, numRec, numRepr);
        }
        else
        {
            return new CountRealPropagatedSelectionMechanism<>(numOwn, numRec);
        }
    } 
}
