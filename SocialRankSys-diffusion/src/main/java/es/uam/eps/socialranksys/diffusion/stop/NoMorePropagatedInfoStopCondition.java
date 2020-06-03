/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.socialranksys.diffusion.stop;

import es.uam.eps.socialranksys.diffusion.data.Data;

import java.io.Serializable;

/**
 * Uses as the end of the simulation the fact that no information has been propagated in the last
 * iteration.
 * @author Javier Sanz-Cruzado Puig.
 * @param <U> type of the users.
 * @param <I> type of the items.
 * @param <P> type of the parameters.
 */
public class NoMorePropagatedInfoStopCondition<U extends Serializable,I extends Serializable,P> implements StopCondition<U,I,P>
{

    @Override
    public boolean stop(int numIter, int numPropagated, int propagatingUsers, long newlyPropagated, long totalPropagated, Data<U, I,P> data, Long timestamp)
    {
        return (numPropagated == 0);
    }
    
}
