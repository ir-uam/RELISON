/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.stop;

import es.uam.eps.ir.socialranksys.diffusion.data.Data;

import java.io.Serializable;

/**
 * Interface for defining stop conditions for simulations.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information
 * @param <P> Type of the parameters
 */
public interface StopCondition<U extends Serializable,I extends Serializable,P>
{
    /**
     * Checks the stop condition.
     * @param numIter Number of iterations.
     * @param numPropagated Number of propagated items.
     * @param propagatingUsers Number of items which propagate information.
     * @param newlyPropagated Newly propagated information pieces
     * @param totalPropagated Total number of propagated tweets.
     * @param data Data
     * @param timestamp the timestamp for the current iteration.
     * @return true if the simulator has to stop, false if not.
     */
    boolean stop(int numIter, int numPropagated, int propagatingUsers, long newlyPropagated, long totalPropagated, Data<U, I, P> data, Long timestamp);
}
