/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.diffusion.sight;

import es.uam.eps.ir.sonalire.diffusion.sight.CountSightMechanism;
import es.uam.eps.ir.sonalire.diffusion.sight.SightMechanism;
import es.uam.eps.ir.sonalire.grid.Parameters;

import java.io.Serializable;

/**
 * Configures a sight mechanism that makes users observe a limited amount of information pieces.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 *
 * @see CountSightMechanism
 */
public class CountSightConfigurator<U extends Serializable,I extends Serializable, F> implements SightConfigurator<U,I, F>
{
    /**
     * Identifier for the number of seen information pieces
     */
    private final static String NUMSIGHT = "numSight";
    
    @Override
    public SightMechanism<U, I, F> configure(Parameters params)
    {
        int numSight = params.getIntegerValue(NUMSIGHT);
        
        return new CountSightMechanism<>(numSight);
    }
    
}
