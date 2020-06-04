/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.stop;


import es.uam.eps.ir.socialranksys.diffusion.stop.StopCondition;

import java.io.Serializable;

/**
 * Interface for configurating a stop condition from a set of parameters.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the items.
 * @param <P> Type of the parameters.
 */
public interface StopConditionConfigurator<U extends Serializable,I extends Serializable,P> 
{
    /**
     * Configures a stop condition from the given parameters
     * @param scgr Set of parameters.
     * @return The configured stop condition.
     */
    StopCondition<U,I,P> getStopCondition(StopConditionParamReader scgr);
}
