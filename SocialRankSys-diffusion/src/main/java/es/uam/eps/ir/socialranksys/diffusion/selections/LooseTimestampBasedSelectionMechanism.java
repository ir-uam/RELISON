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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Selection mechanism that takes the real timestamps of the users into account. Each own piece of information
 * is released when the timestamp of the piece is equal to the timestamp of the simulation. No information pieces
 * are repropagated.
 * 
 * Information pieces from other users are propagated if they are received and the timestamp of the real propagation
 * is smaller or equal than the current one.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <P> type of the parameters.
 */
public class LooseTimestampBasedSelectionMechanism<U extends Serializable, I extends Serializable, P> extends TimestampBasedSelectionMechanism<U,I,P>  
{
    /**
     * Map containing, for each user, the information pieces which should have been propagated
     * in the past (originated in other users), but have not been propagated yet, because
     * the piece was not previously received.
     */
    private final Map<U, Set<Integer>> notProp = new ConcurrentHashMap<>();
    
    @Override
    protected List<PropagatedInformation> getReceivedInformation(UserState<U> user, Data<U, I, P> data, SimulationState<U, I, P> state, int numIter, Long timestamp)
    {
        U u = user.getUserId();
        List<PropagatedInformation> prop = new ArrayList<>();
        
        int uidx = data.getUserIndex().object2idx(user.getUserId());

        // If some piece which should have been propagated has not been received until now, propagate it!
        if(notProp.containsKey(u))
        {
            notProp.get(u).forEach(i -> 
            {
                if(user.containsReceivedInformation(i))
                {
                    prop.add(new PropagatedInformation(i, numIter, uidx));
                    /*notProp.get(u).remove(i);
                    if(notProp.get(u).isEmpty()) notProp.remove(u);*/
                }
            });

            for(PropagatedInformation p : prop)
            {
                notProp.get(u).remove(p.getInfoId());
                if(notProp.get(u).isEmpty()) notProp.remove(u);
            }
        }

        // In case the timestamp is null, there will not be more pieces.
        if(timestamp != null)
        {
            data.getRealPropPiecesByTimestamp(timestamp, u).forEach(i -> 
            {
                int iidx = data.getInformationPiecesIndex().object2idx(i);
                if(user.containsReceivedInformation(iidx))
                {
                    prop.add(new PropagatedInformation(iidx, numIter, uidx));
                }
                else
                {
                    if(!notProp.containsKey(u)) 
                    {
                        notProp.put(u, new HashSet<>());
                    }
                    notProp.get(u).add(iidx);
                }
            });
        }

        return prop;
    }
    
    @Override
    public Stream<U> getSelectableUsers(Data<U, I, P> data, SimulationState<U,I,P> state, int numIter, Long timestamp)
    {
        
        Set<U> set;
        if(timestamp != null) 
        {
            set = data.getUsersByTimestamp(timestamp).collect(Collectors.toCollection(HashSet::new));
            data.getRealPropUsersByTimestamp(timestamp).forEach(set::add);
        }
        else
        {
            set = new HashSet<>();
        }
        set.addAll(notProp.keySet());
        return set.stream();
    } 
}
