/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.simulation;

import es.uam.eps.ir.socialranksys.diffusion.data.Data;
import es.uam.eps.ir.socialranksys.diffusion.data.FastUser;
import es.uam.eps.ir.socialranksys.diffusion.data.PropagatedInformation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Stores the current state of a simulation
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 * @param <I> type of the information pieces
 * @param <P> type of the parameter values
 */
public class SimulationState<U extends Serializable,I extends Serializable,P> 
{
    /**
     * Current state of the users
     */
    private final Map<U, UserState<U>> userState = new HashMap<>();
    
    /**
     * Clears the structures for a new simulation.
     */
    public void clear()
    {
        this.userState.clear();
    }
    
    /**
     * Initializes the different structures for the simulation.
     * @param data the simulation data.
     */
    public void initialize(Data<U,I,P> data)
    {
        this.clear();
        data.getAllUsers().forEach(u -> 
        {
            UserState<U> user = new FastUser<>(u);
            user.resetOwnInformation(data.getPieces(u).map(i -> 
            {
                int uidx = data.getUserIndex().object2idx(u);
                int iidx = data.getInformationPiecesIndex().object2idx(i);
                return new PropagatedInformation(iidx, 0, uidx);
            }));
            userState.put(u, user);
        });
    }
    
    /* *
     * Initializes the different structures for the simulation. In this case, 
     * simulation is not started from zero (there is previous information to consider).
     * @param data the data.
     * @param simulation 
     */
 /*   public void initialize(Data<U,I,P> data, Simulation<U,I,P> simulation)
    {
        this.initialize(data);
        // Get the number of iterations
        int numSims = simulation.getNumIterations();
        
        Map<U, Set<I>> receivedInfo = new HashMap<>();
        Map<U, Set<I>> propagatedInfo = new HashMap<>();
        Map<U, Set<I>> discardedInfo = new HashMap<>();
        
        data.getAllUsers().forEach(u -> 
        {
            receivedInfo.put(u, new HashSet<>());
            propagatedInfo.put(u, new HashSet<>());
            discardedInfo.put(u, new HashSet<>());
        });
        
        for(int i = 0; i < numSims; ++i)
        {
            SimpleIteration<U,I,P> iteration = simulation.getIteration(i);
            
            iteration.getPropagatingUsers().forEach(u -> 
            {
                iteration.getUserPropagatedInformation(u).forEach(info -> receivedInfo.get(u).add(info));
            });
            
            iteration.getDiscardingUsers().forEach(u -> 
            {
                iteration.getUserDiscardedInformation(u).forEach(info -> 
                {
                    receivedInfo.get(u).remove(info);
                    discardedInfo.get(u).add(info);
                });
            });
            
            iteration.getReceivingUsers().forEach(u -> 
            {
                iteration.getUserSeenInformation(u).forEach(info -> 
                {
                    receivedInfo.get(u).add(info);
                });
            });
            
            iteration.getReReceivingUsers().forEach(u -> 
            {
                iteration.getUserReReceivedInformation(u).forEach(info -> 
                {
                    receivedInfo.get(u).add(info);
                });
            });
        }
        
        data.getAllUsers().forEach(u -> 
        {
            UserState<U> user = userState.get(u);
            
            receivedInfo.get(u).forEach(info -> 
            {
                int iidx = data.getInformationPiecesIndex().object2idx(info);
                user.addReceivedInformation(new PropagatedInformation<>(iidx, 0));
            });
        });
    }*/
    
    
    /**
     * Obtains the state of the user.
     * @param u the user.
     * @return the state of the user.
     */
    public UserState<U> getUser(U u)
    {
        return this.userState.get(u);
    }
    
    /**
     * Gets the state of all users.
     * @return the state of all users.
     */
    public Stream<UserState<U>> getAllUsers()
    {
        return userState.values().stream();
    }
}
