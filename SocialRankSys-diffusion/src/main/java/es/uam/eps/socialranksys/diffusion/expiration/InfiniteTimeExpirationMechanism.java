/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.socialranksys.diffusion.expiration;

import es.uam.eps.socialranksys.diffusion.data.Data;
import es.uam.eps.socialranksys.diffusion.simulation.UserState;

import java.io.Serializable;
import java.util.stream.Stream;

/**
 * Expiration mechanism that does not discard any information piece
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 * @param <I> type of the information
 * @param <P> type of the parameters
 */
public class InfiniteTimeExpirationMechanism<U extends Serializable,I extends Serializable,P> implements ExpirationMechanism<U,I,P>
{

    @Override
    public Stream<Integer> expire(UserState<U> user, Data<U,I,P> data, int numIter, Long timestamp)
    {
        return Stream.empty();
    }
    
}
