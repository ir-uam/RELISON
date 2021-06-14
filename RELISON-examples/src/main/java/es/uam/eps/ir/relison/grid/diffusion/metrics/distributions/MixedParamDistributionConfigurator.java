/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.diffusion.metrics.distributions;

import es.uam.eps.ir.relison.diffusion.metrics.distributions.Distribution;
import es.uam.eps.ir.relison.diffusion.metrics.distributions.MixedFeatureDistribution;
import es.uam.eps.ir.relison.grid.Parameters;

import java.io.Serializable;
/**
 * Class for configuring a distribution of two mixed parameters (one from information pieces,
 * and other from users).
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user / information features.
 *
 * @see MixedFeatureDistribution
 */
public class MixedParamDistributionConfigurator<U extends Serializable,I extends Serializable, F> implements DistributionConfigurator<U,I, F>
{
    /**
     * Identifier for the selected user feature.
     */
    private final static String USERFEAT = "userFeature";
    /**
     * Identifier for the selected information pieces feature
     */
    private final static String INFOFEAT = "infoFeature";
    
    @Override
    public Distribution<U, I, F> configure(Parameters params)
    {
        String userParameter = params.getStringValue(USERFEAT);
        String infoParameter = params.getStringValue(INFOFEAT);
        return new MixedFeatureDistribution<>(infoParameter, userParameter);
    }
}
