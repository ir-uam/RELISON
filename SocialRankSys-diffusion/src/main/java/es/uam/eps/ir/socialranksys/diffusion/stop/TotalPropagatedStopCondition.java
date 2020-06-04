/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
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
 * Stops after a given number of information pieces has been propagated.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 * @param <I> type of the items
 * @param <P> type of the parameters
 */
public class TotalPropagatedStopCondition<U extends Serializable,I extends Serializable,P> implements StopCondition<U,I,P>
{

    /**
     * Maximum number of propagated pieces.
     */
    private final long limit;
    
    /**
     * Constructor.
     * @param limit Maximum number of propagated pieces.
     */
    public TotalPropagatedStopCondition(long limit)
    {
        this.limit = limit;
    }
    
    @Override
    public boolean stop(int numIter, int numPropagated, int propagatingUsers, long newlyPropagated, long totalPropagated, Data<U, I, P> data, Long timestamp)
    {
        return totalPropagated > this.limit || numPropagated == 0; 
    }
    
}
