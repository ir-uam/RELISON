/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.diffusion.expiration;

import es.uam.eps.ir.sonalire.diffusion.data.Data;
import es.uam.eps.ir.sonalire.diffusion.data.Information;
import es.uam.eps.ir.sonalire.diffusion.simulation.UserState;

import java.io.Serializable;
import java.util.stream.Stream;

/**
 * If current timestamp is greater than the timestamp of the pieces, the elements 
 * are discarded.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the features.
 */
public class AllNotRealPropagatedTimestampExpirationMechanism<U extends Serializable, I extends Serializable, P> implements ExpirationMechanism<U,I,P> 
{
    
    @Override
    public Stream<Integer> expire(UserState<U> user, Data<U,I,P> data, int numIter, Long timestamp)
    {
        U u = user.getUserId();
        if(timestamp != null)
        {
            return user.getReceivedInformation().filter(piece -> 
            {
                I i = data.getInformationPiecesIndex().idx2object(piece.getInfoId());
                return !(data.isRealRepropagatedPiece(user.getUserId(), i) && data.getRealPropagatedTimestamp(u, i) >= timestamp);
            }).map(Information::getInfoId);
        }
        else //If there is no timestamp, we assume that all pieces were created before the timestamp
        {
            return user.getReceivedInformationIds();
        }
    }
}
