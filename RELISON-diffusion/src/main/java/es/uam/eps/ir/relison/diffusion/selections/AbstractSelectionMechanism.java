/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.selections;

import es.uam.eps.ir.relison.diffusion.data.Data;
import es.uam.eps.ir.relison.diffusion.data.PropagatedInformation;
import es.uam.eps.ir.relison.diffusion.simulation.SimulationState;
import es.uam.eps.ir.relison.diffusion.simulation.UserState;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Stream;

/**
 * Abstract implementation of a selection mechanism.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <P> type of the parameters.
 */
public abstract class AbstractSelectionMechanism<U extends Serializable, I extends Serializable, P> implements SelectionMechanism<U,I,P> 
{
    @Override
    public Selection select(UserState<U> user, Data<U, I, P> data, SimulationState<U, I, P> state, int numIter, Long timestamp)
    {
        // For each user, we select:
        // a) some pieces of information originally owned by the user.
        List<PropagatedInformation> ownInfo = this.getOwnInformation(user, data, state, numIter, timestamp);
        // b) some pieces of information received by the user.
        List<PropagatedInformation> recInfo = this.getReceivedInformation(user, data, state, numIter, timestamp);
        // c) some pieces of information that the user did propagate earlier, but now wants to propagate again.
        List<PropagatedInformation> repropInfo = this.getRepropagatedInformation(user, data, state, numIter, timestamp);

        return new Selection(ownInfo,recInfo,repropInfo);
    }

    /**
     * Obtains the list of own information pieces to repropagate.
     * @param user      the user to analyze.
     * @param data      the full data.
     * @param state     current simulation state.
     * @param numIter   the iteration number.
     * @param timestamp the timestamp for the current simulation.
     * @return a selection of the own information pieces to be propagated.
     */
    protected abstract List<PropagatedInformation> getOwnInformation(UserState<U> user, Data<U, I, P> data, SimulationState<U, I, P> state, int numIter, Long timestamp);
   
    /**
     * Obtains the list of received information pieces to repropagate.
     * @param user      the user to analyze.
     * @param data      the full data.
     * @param state     the iteration number.
     * @param numIter   number of the iteration.
     * @param timestamp the timestamp for the current simulation.
     * @return a selection of the received information pieces to be propagated.
     */
    protected abstract List<PropagatedInformation> getReceivedInformation(UserState<U> user, Data<U, I, P> data, SimulationState<U, I, P> state, int numIter, Long timestamp);
    
    /**
     * Obtains the list of propagated information pieces to repropagate.
     * @param user the user to analyze.
     * @param data the full data.
     * @param state current simulation state.
     * @param numIter number of the iteration.
     * @param timestamp the timestamp for the current simulation.
     * @return a selection of the information pieces to repropagate.
     */
    protected abstract List<PropagatedInformation> getRepropagatedInformation(UserState<U> user, Data<U, I, P> data, SimulationState<U, I, P> state, int numIter, Long timestamp);

    @Override
    public Stream<U> getSelectableUsers(Data<U, I, P> data, SimulationState<U,I,P> state, int numIter, Long timestamp)
    {
        return data.getAllUsers();
    }
}
