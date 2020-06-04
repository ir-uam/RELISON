/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.stop;


import es.uam.eps.socialranksys.diffusion.stop.MaxTimestampStopCondition;
import es.uam.eps.socialranksys.diffusion.stop.StopCondition;

import java.io.Serializable;

/**
 * Configurator for a Max Timestamp stop condition.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 * @param <I> Type of the information pieces
 * @param <P> Type of the features.
 * @see es.uam.eps.socialranksys.diffusion.stop.MaxTimestampStopCondition
 */
public class MaxTimestampStopConditionConfigurator<U extends Serializable,I extends Serializable,P> implements StopConditionConfigurator<U,I,P>
{
    
    private final static String MAX = "maxTimestamp"; 
    @Override
    public StopCondition<U, I, P> getStopCondition(StopConditionParamReader scgr)
    {
        long maxTimestamp = scgr.getParams().getLongValue(MAX);
        return new MaxTimestampStopCondition<>(maxTimestamp);
    }   
}
