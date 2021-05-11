/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.stop;

import es.uam.eps.ir.socialranksys.diffusion.stop.StopCondition;
import es.uam.eps.ir.socialranksys.diffusion.stop.TotalPropagatedStopCondition;
import es.uam.eps.ir.socialranksys.grid.Parameters;

import java.io.Serializable;

/**
 * Configures an stop condition that finishes after a given amount of information pieces have been propagated.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 */
public class TotalPropagatedStopConditionConfigurator<U extends Serializable,I extends Serializable, F> implements StopConditionConfigurator<U,I, F>
{
    /**
     * Identifier for the number of information pieces to propagate before stopping.
     */
    private final static String PROPAGATED = "propagated";
    
    @Override
    public StopCondition<U,I, F> getStopCondition(Parameters scgr)
    {
        long totalPieces = scgr.getLongValue(PROPAGATED);
        return new TotalPropagatedStopCondition<>(totalPieces);
    }
}
