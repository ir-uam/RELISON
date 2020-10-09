/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Selection mechanism that takes the real timestamps of the users into account. Each own piece of information
 * is released when the timestamp of the piece is equal to the timestamp of the simulation. No information pieces
 * are repropagated.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public class PureTimestampBasedSelectionMechanism<U extends Serializable, I extends Serializable, P> extends TimestampBasedSelectionMechanism<U,I,P>  
{
    @Override
    protected List<PropagatedInformation> getReceivedInformation(UserState<U> user, Data<U, I, P> data, SimulationState<U, I, P> state, int numIter, Long timestamp)
    {
        List<PropagatedInformation> prop = new ArrayList<>();

        U u = user.getUserId();
        int uidx = data.getUserIndex().object2idx(user.getUserId());
        
        data.getRealPropPiecesByTimestamp(timestamp, u).forEach(i -> 
        {
            int iidx = data.getInformationPiecesIndex().object2idx(i);
            if(user.containsReceivedInformation(iidx))
            {
                prop.add(new PropagatedInformation(iidx, numIter, uidx));
            }
        });
        return prop;
    }
    
    @Override
    public Stream<U> getSelectableUsers(Data<U, I, P> data, SimulationState<U,I,P> state, int numIter, Long timestamp)
    {
        Set<U> set = data.getUsersByTimestamp(timestamp).collect(Collectors.toCollection(HashSet::new));
        data.getRealPropUsersByTimestamp(timestamp).forEach(set::add);
        return set.stream();
    } 
}
