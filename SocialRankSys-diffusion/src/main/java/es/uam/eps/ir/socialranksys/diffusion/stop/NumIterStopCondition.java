/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.stop;

import es.uam.eps.ir.socialranksys.diffusion.data.Data;

import java.io.Serializable;

/**
 * Stops after a given number of iterations.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the items.
 * @param <P> type of the parameters.
 */
public class NumIterStopCondition<U extends Serializable,I extends Serializable,P> implements StopCondition<U,I,P>
{

    /**
     * Maximum number of iterations.
     */
    private final int limit;
    /**
     * Constructor.
     * @param limit Maximum number of iterations. 
     */
    public NumIterStopCondition(int limit)
    {
        this.limit = limit;
    }
    
    @Override
    public boolean stop(int numIter, int numPropagated, int propagatingUsers, long newlyPropagated, long totalPropagated, Data<U, I, P> data, Long timestamp)
    {
        return numIter > this.limit; 
    }
    
}
