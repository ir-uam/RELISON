/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.stop;

import es.uam.eps.ir.relison.diffusion.data.Data;

import java.io.Serializable;

/**
 * Interface for defining stop conditions for simulations.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 */
public interface StopCondition<U extends Serializable,I extends Serializable, F>
{
    /**
     * Checks the stop condition.
     * @param numIter           the current number of iterations.
     * @param numPropagated     the number of information pieces which has been propagated during this iteration.
     * @param propagatingUsers  the number of users who propagate information.
     * @param newlyPropagated   the number of information pieces which have been seen this iteration.
     * @param totalPropagated   the total number of information pieces which have been propagated during the simulation.
     * @param data              the simulation data.
     * @param timestamp         the timestamp corresponding to the current iteration.
     * @return true if the simulator has to stop, false if not.
     */
    boolean stop(int numIter, int numPropagated, int propagatingUsers, long newlyPropagated, long totalPropagated, Data<U, I, F> data, Long timestamp);
}
