/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.metrics.features.global;

import es.uam.eps.ir.socialranksys.diffusion.metrics.SimulationMetric;
import es.uam.eps.ir.socialranksys.diffusion.metrics.features.global.UserFeatureGini;
import es.uam.eps.ir.socialranksys.grid.diffusion.metrics.MetricConfigurator;
import es.uam.eps.ir.socialranksys.grid.diffusion.metrics.MetricParamReader;

import java.io.Serializable;

/**
 * Configurator for the User-Feature Gini metric.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the features.
 */
public class UserFeatureGiniMetricConfigurator<U extends Serializable,I extends Serializable,P> implements MetricConfigurator<U,I,P>
{
    /**
     * Identifier for the parameter that indicates if a feature refers to a user or to an information piece.
     */
    private final static String USERFEAT = "userFeature";
    /**
     * Identifier for the name of the feature.
     */
    private final static String PARAM = "parameter";
    /**
     * Identifier for considering unique user-item pairs or not.
     */
    private final static String UNIQUE = "unique";
    
    @Override
    public SimulationMetric<U, I, P> configure(MetricParamReader params)
    {
        Boolean userParam = params.getParams().getBooleanValue(USERFEAT);
        String param = params.getParams().getStringValue(PARAM);
        Boolean unique = params.getParams().getBooleanValue(UNIQUE);
        
        if(userParam == null || param == null || unique == null)
        {
            return null;
        }
        
        return new UserFeatureGini<>(param, userParam, unique);
    } 
    
}
