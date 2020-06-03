/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.uam.eps.socialranksys.diffusion.expiration;

import es.uam.eps.socialranksys.diffusion.data.Data;
import es.uam.eps.socialranksys.diffusion.data.Information;
import es.uam.eps.socialranksys.diffusion.simulation.UserState;

import java.io.Serializable;
import java.util.stream.Stream;

/**
 * Expiration mechanism that removes every information piece that has not been
 * repropagated in a real life scenario.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 * @param <I> type of the information
 * @param <P> type of the parameters
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