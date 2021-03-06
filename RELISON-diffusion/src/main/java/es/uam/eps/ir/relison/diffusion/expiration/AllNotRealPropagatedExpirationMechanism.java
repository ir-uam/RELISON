/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.expiration;

import es.uam.eps.ir.relison.diffusion.data.Data;
import es.uam.eps.ir.relison.diffusion.data.Information;
import es.uam.eps.ir.relison.diffusion.simulation.UserState;

import java.io.Serializable;
import java.util.stream.Stream;

/**
 * Expiration mechanism that removes every information piece that has not been
 * repropagated in a real life scenario.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information.
 * @param <P> type of the parameters.
 */
public class AllNotRealPropagatedExpirationMechanism<U extends Serializable,I extends Serializable,P> implements ExpirationMechanism<U,I,P>
{   
    @Override
    public Stream<Integer> expire(UserState<U> user, Data<U,I,P> data, int numIter, Long timestamp)
    {
        return user.getReceivedInformation().filter(piece -> 
        {
            I i = data.getInformationPiecesIndex().idx2object(piece.getInfoId());
            return !data.isRealRepropagatedPiece(user.getUserId(), i);
        }).map(Information::getInfoId);
    }    
}