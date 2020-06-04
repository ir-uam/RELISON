/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.metrics.features.global;

import es.uam.eps.ir.socialranksys.grid.diffusion.metrics.MetricConfigurator;
import es.uam.eps.ir.socialranksys.grid.diffusion.metrics.MetricParamReader;
import es.uam.eps.socialranksys.diffusion.metrics.SimulationMetric;
import es.uam.eps.socialranksys.diffusion.metrics.features.global.FeatureGlobalUserEntropy;

import java.io.Serializable;

/**
 * Configures a global feature user entropy metric.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public class FeatureGlobalUserEntropyMetricConfigurator<U extends Serializable,I extends Serializable,P> implements MetricConfigurator<U,I,P>
{
    /**
     * Identifier for the parameter name
     */
    private final static String PARAMETER = "parameter";
    /**
     * Identifier for the param which identifies if the studied parameter is an user or an information piece feature.
     */
    private final static String USERPARAM = "userFeature";
    
    @Override
    public SimulationMetric<U, I, P> configure(MetricParamReader params)
    {
        String parameter = params.getParams().getStringValue(PARAMETER);
        Boolean userParam = params.getParams().getBooleanValue(USERPARAM);
        
        if(parameter == null || userParam == null)
            return null;
        
        return new FeatureGlobalUserEntropy<>(parameter, userParam);
    }
    
}
