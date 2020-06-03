/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.socialranksys.diffusion.selections;

import es.uam.eps.socialranksys.diffusion.data.Data;
import es.uam.eps.socialranksys.diffusion.data.PropagatedInformation;
import es.uam.eps.socialranksys.diffusion.simulation.SimulationState;
import es.uam.eps.socialranksys.diffusion.simulation.UserState;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Stream;

/**
 * Abstract selection mechanism.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public abstract class AbstractSelectionMechanism<U extends Serializable, I extends Serializable, P> implements SelectionMechanism<U,I,P> 
{
    @Override
    public Selection select(UserState<U> user, Data<U, I, P> data, SimulationState<U, I, P> state, int numIter, Long timestamp)
    {
        List<PropagatedInformation> ownInfo = this.getOwnInformation(user, data, state, numIter, timestamp);
        List<PropagatedInformation> recInfo = this.getReceivedInformation(user, data, state, numIter, timestamp);
        List<PropagatedInformation> repropInfo = this.getRepropagatedInformation(user, data, state, numIter, timestamp);
        return new Selection(ownInfo,recInfo,repropInfo);
    }

    /**
     * Obtains the list of own information pieces to repropagate.
     * @param user the user to analyze
     * @param data the full data
     * @param state current simulation state
     * @param numIter number of the iteration
     * @param timestamp the timestamp for the current simulation
     * @return a selection of the own tweets to be propagated
     */
    protected abstract List<PropagatedInformation> getOwnInformation(UserState<U> user, Data<U, I, P> data, SimulationState<U, I, P> state, int numIter, Long timestamp);
   
    /**
     * Obtains the list of received information pieces to repropagate.
     * @param user the user to analyze
     * @param data the full data
     * @param state current simulation state
     * @param numIter number of the iteration
     * @param timestamp the timestamp for the current simulation
     * @return a selection of the own tweets to be propagated
     */
    protected abstract List<PropagatedInformation> getReceivedInformation(UserState<U> user, Data<U, I, P> data, SimulationState<U, I, P> state, int numIter, Long timestamp);
    
    /**
     * Obtains the list of propagated information pieces to repropagate.
     * @param user the user to analyze
     * @param data the full data
     * @param state current simulation state
     * @param numIter number of the iteration
     * @param timestamp the timestamp for the current simulation
     * @return a selection of the own tweets to be propagated
     */
    protected abstract List<PropagatedInformation> getRepropagatedInformation(UserState<U> user, Data<U, I, P> data, SimulationState<U, I, P> state, int numIter, Long timestamp);

    @Override
    public Stream<U> getSelectableUsers(Data<U, I, P> data, SimulationState<U,I,P> state, int numIter, Long timestamp)
    {
        return data.getAllUsers();
    }
}
