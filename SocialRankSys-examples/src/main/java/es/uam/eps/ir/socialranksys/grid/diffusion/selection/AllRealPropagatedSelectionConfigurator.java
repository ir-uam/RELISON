/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.selection;

import es.uam.eps.socialranksys.diffusion.selections.AllRealPropagatedSelectionMechanism;
import es.uam.eps.socialranksys.diffusion.selections.SelectionMechanism;

import java.io.Serializable;

/**
 * Configures a All Real Propagated selection mechanism.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 * 
 * @see es.uam.eps.socialranksys.diffusion.selections.AllRealPropagatedSelectionMechanism
 */
public class AllRealPropagatedSelectionConfigurator<U extends Serializable,I extends Serializable,P> implements SelectionConfigurator<U,I,P>
{
    /**
     * Identifier for the number of own pieces of information to propagate.
     */
    private final static String NUMOWN = "numOwn";
    
    /**
     * Identifier for the number of propagated pieces of information to repropagate.
     */
    private final static String NUMREPR = "numRepr";
    
    
    @Override
    public SelectionMechanism<U,I,P> configure(SelectionParamReader params)
    {
        int numOwn = params.getParams().getIntegerValue(NUMOWN);
        if(params.getParams().getIntegerValue(NUMREPR) != null)
        {
            int numRepr = params.getParams().getIntegerValue(NUMREPR);
            return new AllRealPropagatedSelectionMechanism<>(numOwn, numRepr);
        }
        else
        {
            return new AllRealPropagatedSelectionMechanism<>(numOwn);
        }
    }
    
}
