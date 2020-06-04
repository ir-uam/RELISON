/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.selections;


import es.uam.eps.ir.socialranksys.diffusion.data.Data;
import es.uam.eps.ir.socialranksys.diffusion.data.PropagatedInformation;
import es.uam.eps.ir.socialranksys.diffusion.simulation.SimulationState;
import es.uam.eps.ir.socialranksys.diffusion.simulation.UserState;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Selection mechanism that takes the real timestamps of the users into account. Each own piece of information
 * is released when the timestamp of the piece is equal to the timestamp of the simulation. No information pieces
 * are repropagated.
 * 
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public abstract class TimestampBasedSelectionMechanism<U extends Serializable, I extends Serializable, P> extends AbstractSelectionMechanism<U,I,P>  
{
    @Override
    protected List<PropagatedInformation> getOwnInformation(UserState<U> user, Data<U, I, P> data, SimulationState<U, I, P> state, int numIter, Long timestamp)
    {
        List<PropagatedInformation> prop = new ArrayList<>();

        int uidx = data.getUserIndex().object2idx(user.getUserId());
        if(timestamp != null)
        {
            data.getPiecesByTimestamp(timestamp, user.getUserId()).forEach(i -> 
            {
                int iidx = data.getInformationPiecesIndex().object2idx(i);
                if(user.containsOwnInformation(iidx))
                {
                    prop.add(new PropagatedInformation(iidx, numIter, uidx));
                }
            });
        }
                
        return prop;
    }

    @Override
    protected List<PropagatedInformation> getRepropagatedInformation(UserState<U> user, Data<U, I, P> data, SimulationState<U, I, P> state, int numIter, Long timestamp) 
    {
        return new ArrayList<>();
    }
    
}
