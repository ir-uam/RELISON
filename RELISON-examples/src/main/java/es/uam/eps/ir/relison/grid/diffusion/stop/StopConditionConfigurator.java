/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.diffusion.stop;


import es.uam.eps.ir.relison.diffusion.stop.StopCondition;
import es.uam.eps.ir.relison.grid.Parameters;

import java.io.Serializable;

/**
 * Interface for the configuration of stop conditions for information diffusion.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> Type of the user and information pieces features.
 */
public interface StopConditionConfigurator<U extends Serializable,I extends Serializable, F>
{
    /**
     * Configures a stop condition from the given parameters.
     * @param scgr the set of parameters for configuring the stop condition.
     * @return the configured stop condition.
     */
    StopCondition<U,I,F> getStopCondition(Parameters scgr);
}
