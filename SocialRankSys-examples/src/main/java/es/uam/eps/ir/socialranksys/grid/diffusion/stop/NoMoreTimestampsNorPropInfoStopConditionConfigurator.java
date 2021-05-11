/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.stop;

import es.uam.eps.ir.socialranksys.diffusion.stop.NoMoreTimestampsNorPropInfoStopCondition;
import es.uam.eps.ir.socialranksys.diffusion.stop.StopCondition;
import es.uam.eps.ir.socialranksys.grid.Parameters;

import java.io.Serializable;

/**
 * Configures an stop condition that finishes the diffusion procedure as it reaches the timestamp of the
 * last propagated information piece (according to the diffusion timestamps of a real procedure), and no
 * more information is propagated.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 */
public class NoMoreTimestampsNorPropInfoStopConditionConfigurator<U extends Serializable, I extends Serializable, F> implements StopConditionConfigurator<U,I,F>
{
    @Override
    public StopCondition<U, I, F> getStopCondition(Parameters scgr)
    {
        return new NoMoreTimestampsNorPropInfoStopCondition<>();
    }   
}
