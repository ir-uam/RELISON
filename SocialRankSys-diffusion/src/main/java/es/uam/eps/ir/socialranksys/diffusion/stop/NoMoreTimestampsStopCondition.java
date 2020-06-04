/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
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
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public class NoMoreTimestampsStopCondition<U extends Serializable,I extends Serializable,P> implements StopCondition<U,I,P>
{
    @Override
    public boolean stop(int numIter, int numPropagated, int propagatingUsers, long newlyPropagated, long totalPropagated, Data<U, I, P> data, Long timestamp)
    {
        return timestamp == null;
    }
    
}
