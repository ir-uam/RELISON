/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.stop;

import es.uam.eps.ir.socialranksys.diffusion.data.Data;

import java.io.Serializable;

/**
 * Stop condition: When the last timestamp has appeared, then, stop.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 */
public class NoMoreTimestampsNorPropInfoStopCondition<U extends Serializable,I extends Serializable, F> implements StopCondition<U,I, F>
{
    @Override
    public boolean stop(int numIter, int numPropagated, int propagatingUsers, long newlyPropagated, long totalPropagated, Data<U, I, F> data, Long timestamp)
    {
        return (timestamp == null && numPropagated == 0);
    }
    
}
