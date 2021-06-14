/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.selections;

import es.uam.eps.ir.relison.diffusion.data.Data;
import es.uam.eps.ir.relison.diffusion.simulation.SimulationState;
import es.uam.eps.ir.relison.diffusion.simulation.UserState;

import java.io.Serializable;
import java.util.stream.Stream;

/**
 * Interface for selecting, each iteration of a diffusion process, the set of users that might propagate
 * some information and the information pieces each one of them might propagate.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information.
 * @param <P> type of the parameters
 */
public interface SelectionMechanism<U extends Serializable,I extends Serializable,P>
{
    /**
     * Given a user, selects the information pieces that he/she propagates during this iteration.
     * @param user      the user to analyze.
     * @param data      the complete data.
     * @param state     the current state of the simulation.
     * @param numIter   the iteration number.
     * @param timestamp the current timestamp.
     * @return a selection of information pieces to be propagated.
     */
    Selection select(UserState<U> user, Data<U, I, P> data, SimulationState<U, I, P> state, int numIter, Long timestamp);

    /**
     * Selects the users which can propagate information during the iteration.
     * @param data      the complete data.
     * @param state     the current state of the simulation.
     * @param numIter   iteration number.
     * @param timestamp the current timestamp.
     * @return a stream containing the users who can propagate information.
     */
    Stream<U> getSelectableUsers(Data<U, I, P> data, SimulationState<U, I, P> state, int numIter, Long timestamp);
}
