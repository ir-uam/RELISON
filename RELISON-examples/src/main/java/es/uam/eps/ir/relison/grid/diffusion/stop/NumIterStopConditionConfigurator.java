/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.diffusion.stop;


import es.uam.eps.ir.relison.diffusion.stop.NumIterStopCondition;
import es.uam.eps.ir.relison.diffusion.stop.StopCondition;
import es.uam.eps.ir.relison.grid.Parameters;

import java.io.Serializable;

/**
 * Configures an stop condition that finishes the diffusion procedure after a fixed number of iterations.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 */
public class NumIterStopConditionConfigurator<U extends Serializable,I extends Serializable,F> implements StopConditionConfigurator<U,I,F>
{
    /**
     * Identifier for the number of iterations.
     */
    private final static String NUMITER = "numIter";
    
    @Override
    public StopCondition<U,I,F> getStopCondition(Parameters scgr)
    {
        int numIter = scgr.getIntegerValue(NUMITER);
        return new NumIterStopCondition<>(numIter);
    }
}
