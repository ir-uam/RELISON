/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.metrics.users;

import es.uam.eps.ir.socialranksys.diffusion.metrics.SimulationMetric;
import es.uam.eps.ir.socialranksys.diffusion.metrics.user.global.UserGlobalGini;
import es.uam.eps.ir.socialranksys.grid.diffusion.metrics.MetricConfigurator;
import es.uam.eps.ir.socialranksys.grid.diffusion.metrics.MetricParamReader;

import java.io.Serializable;

/**
 * Configures a User Global Gini metric.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public class UserGlobalGiniMetricConfigurator<U extends Serializable,I extends Serializable,P> implements MetricConfigurator<U,I,P>
{
    
    /**
     * Identifier for considering unique user-item pairs or not.
     */
    private final static String UNIQUE = "unique";
    
    @Override
    public SimulationMetric<U, I, P> configure(MetricParamReader params)
    {
        Boolean unique = params.getParams().getBooleanValue(UNIQUE);
        
        if(unique == null)
        {
            return null;
        }
        
        return new UserGlobalGini<>(unique);
    }
    
}
