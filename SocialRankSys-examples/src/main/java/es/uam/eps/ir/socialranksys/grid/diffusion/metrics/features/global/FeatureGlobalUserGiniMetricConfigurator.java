/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.metrics.features.global;

import es.uam.eps.ir.socialranksys.diffusion.metrics.SimulationMetric;
import es.uam.eps.ir.socialranksys.diffusion.metrics.features.global.FeatureGlobalUserGini;
import es.uam.eps.ir.socialranksys.grid.Parameters;
import es.uam.eps.ir.socialranksys.grid.diffusion.metrics.MetricConfigurator;

import java.io.Serializable;

/**
 * Configures a metric that measures the complement of the Gini coefficient over the different features, where, for each feature,
 * we measure the number of users who have received it.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user / information features.
 *
 * @see FeatureGlobalUserGini
 */
public class FeatureGlobalUserGiniMetricConfigurator<U extends Serializable,I extends Serializable, F> implements MetricConfigurator<U,I, F>
{
    /**
     * Identifier for the feature name
     */
    private final static String FEATURE = "feature";
    /**
     * Identifier for the param which identifies if the studied feature is an user or an information piece feature.
     */
    private final static String USERFEAT = "userFeature";

    @Override
    public SimulationMetric<U, I, F> configure(Parameters params)
    {
        String feature = params.getStringValue(FEATURE);
        Boolean userFeat = params.getBooleanValue(USERFEAT);

        if(feature == null || userFeat == null)
            return null;

        return new FeatureGlobalUserGini<>(feature, userFeat);
    }

}
