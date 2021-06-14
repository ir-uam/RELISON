/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.diffusion.metrics.features.global;

import es.uam.eps.ir.relison.diffusion.metrics.SimulationMetric;
import es.uam.eps.ir.relison.diffusion.metrics.features.global.UserFeatureCount;
import es.uam.eps.ir.relison.grid.Parameters;
import es.uam.eps.ir.relison.grid.diffusion.metrics.MetricConfigurator;

import java.io.Serializable;

/**
 * Configures a metric that measures the number of different (user, feature) pairs.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user / information features.
 *
 * @see UserFeatureCount
 */
public class UserFeatureCountConfigurator<U extends Serializable,I extends Serializable, F> implements MetricConfigurator<U,I, F>
{
    /**
     * Identifier for the parameter that indicates if a feature refers to a user or to an information piece.
     */
    private final static String USERFEAT = "userFeature";
    /**
     * Identifier for the name of the feature.
     */
    private final static String FEATURE = "feature";
    
    @Override
    public SimulationMetric<U, I, F> configure(Parameters params)
    {
        Boolean userFEat = params.getBooleanValue(USERFEAT);
        String feature = params.getStringValue(FEATURE);
        
        if(userFEat == null || feature == null)
        {
            return null;
        }
        
        return new UserFeatureCount<>(feature, userFEat);
    } 
    
}
