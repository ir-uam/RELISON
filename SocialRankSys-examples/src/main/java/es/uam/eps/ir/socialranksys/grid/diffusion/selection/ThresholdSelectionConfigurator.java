/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.selection;

import es.uam.eps.ir.socialranksys.diffusion.selections.SelectionMechanism;
import es.uam.eps.ir.socialranksys.diffusion.selections.ThresholdSelectionMechanism;

import java.io.Serializable;

/**
 * Configures a Threshold Model selection mechanism
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 * @see ThresholdSelectionMechanism
 */
public class ThresholdSelectionConfigurator<U extends Serializable,I extends Serializable,P> implements SelectionConfigurator<U,I,P>
{
    /**
     * Identifier for the number of own elements to propagate.
     */
    private final static String NUMOWN = "numOwn";
    /**
     * Identifier for the number of elements to repropagate.
     */
    private final static String NUMREC = "numRec";
    /**
     * Identifier for the proportion of elements needed for repropagating an individual piece.
     */
    private final static String THRESHOLD = "threshold";
    
    
    @Override
    public SelectionMechanism<U, I, P> configure(SelectionParamReader params)
    {
        int numOwn = params.getParams().getIntegerValue(NUMOWN);
        int numRec = params.getParams().getIntegerValue(NUMREC);

        double threshold = params.getParams().getDoubleValue(THRESHOLD);
        
        return new ThresholdSelectionMechanism<>(numOwn, numRec, threshold);
    }
    
    
}
