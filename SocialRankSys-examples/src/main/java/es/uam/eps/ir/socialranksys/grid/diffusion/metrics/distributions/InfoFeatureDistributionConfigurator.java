/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.metrics.distributions;

import es.uam.eps.ir.socialranksys.diffusion.metrics.distributions.Distribution;
import es.uam.eps.ir.socialranksys.diffusion.metrics.distributions.InformationFeatureDistribution;
import es.uam.eps.ir.socialranksys.grid.Parameters;

import java.io.Serializable;

/**
 * Configures a distribution measuring the number of times each information pieces feature has been received.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user / information features.
 *
 * @see InformationFeatureDistribution
 */
public class InfoFeatureDistributionConfigurator<U extends Serializable,I extends Serializable, F> implements DistributionConfigurator<U,I, F>
{
    /**
     * Identifier for the selected feature.
     */
    private final static String FEATURE = "feature";
    @Override
    public Distribution<U, I, F> configure(Parameters params)
    {
        String feature = params.getStringValue(FEATURE);
        return new InformationFeatureDistribution<>(feature);
    }
    
}
