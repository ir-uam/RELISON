/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.diffusion.metrics.features.global;

import es.uam.eps.ir.sonalire.diffusion.metrics.SimulationMetric;
import es.uam.eps.ir.sonalire.diffusion.metrics.features.global.GlobalExternalFeatureRate;
import es.uam.eps.ir.sonalire.grid.Parameters;
import es.uam.eps.ir.sonalire.grid.diffusion.metrics.MetricConfigurator;

import java.io.Serializable;

/**
 * Configures a metric that measures the proportion of the received features unknown to the
 * users (globally)
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user / information features.
 *
 * @see GlobalExternalFeatureRate
 */
public class GlobalExternalFeatureRateMetricConfigurator<U extends Serializable,I extends Serializable, F> implements MetricConfigurator<U,I, F>
{
    /**
     * Identifier for the feature name
     */
    private final static String FEATURE = "feature";
    /**
     * Identifier for the param which identifies if the studied feature is an user or an information piece feature.
     */
    private final static String USERFEAT = "userFeature";
    /**
     * Identifier for considering unique user-item pairs or not.
     */
    private final static String UNIQUE = "unique";
    
    @Override
    public SimulationMetric<U, I, F> configure(Parameters params)
    {
        String feature = params.getStringValue(FEATURE);
        Boolean userFeat = params.getBooleanValue(USERFEAT);
        Boolean unique = params.getBooleanValue(UNIQUE);
        
        if(feature == null || userFeat == null || unique == null)
            return null;
        
        return new GlobalExternalFeatureRate<>(feature, userFeat, unique);
    }
    
}
