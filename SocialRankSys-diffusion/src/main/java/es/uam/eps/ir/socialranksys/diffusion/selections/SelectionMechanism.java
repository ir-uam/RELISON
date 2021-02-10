/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.selections;

import es.uam.eps.ir.socialranksys.diffusion.data.Data;
import es.uam.eps.ir.socialranksys.diffusion.simulation.SimulationState;
import es.uam.eps.ir.socialranksys.diffusion.simulation.UserState;

import java.io.Serializable;
import java.util.stream.Stream;

/**
 * Given a list of tweets and a list of possible retweets, select which one we want to propagate.
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
     * Selects the information pieces to be propagated.
     * @param user The user to analyze.
     * @param data Full data.
     * @param state Current simulation state.
     * @param numIter Number of the iteration.
     * @param timestamp the current timestamp.
     * @return A selection of tweets to be propagated.
     */
    Selection select(UserState<U> user, Data<U, I, P> data, SimulationState<U, I, P> state, int numIter, Long timestamp);

    /**
     * Selects the users which can propagate information during the iteration.
     * @param data the data.
     * @param state the state of the simulation.
     * @param numIter iteration number.
     * @param timestamp the current timestamp.
     * @return a stream containing those users.
     */
    Stream<U> getSelectableUsers(Data<U, I, P> data, SimulationState<U, I, P> state, int numIter, Long timestamp);
}
