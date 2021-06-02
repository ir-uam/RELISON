/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.diffusion.stop;


import es.uam.eps.ir.sonalire.diffusion.stop.NoMoreNewPropagatedInfoStopCondition;
import es.uam.eps.ir.sonalire.diffusion.stop.StopCondition;
import es.uam.eps.ir.sonalire.grid.Parameters;

import java.io.Serializable;

/**
 * Configures an stop condition that establishes that the diffusion ends when no new information is propagated.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 */
public class NoMoreNewStopConditionConfigurator<U extends Serializable,I extends Serializable, F> implements StopConditionConfigurator<U,I, F>
{
    @Override
    public StopCondition<U,I, F> getStopCondition(Parameters scgr)
    {
        return new NoMoreNewPropagatedInfoStopCondition<>();
    }
}
