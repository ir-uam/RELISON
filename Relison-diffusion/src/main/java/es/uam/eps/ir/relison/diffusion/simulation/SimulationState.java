/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.simulation;

import es.uam.eps.ir.relison.diffusion.data.Data;
import es.uam.eps.ir.relison.diffusion.data.FastUser;
import es.uam.eps.ir.relison.diffusion.data.PropagatedInformation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Stores the current state of a simulation.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> Type of the users.
 * @param <I> type of the information pieces.
 * @param <P> type of the parameter values.
 */
public class SimulationState<U extends Serializable,I extends Serializable,P> 
{
    /**
     * Current state of the users.
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
