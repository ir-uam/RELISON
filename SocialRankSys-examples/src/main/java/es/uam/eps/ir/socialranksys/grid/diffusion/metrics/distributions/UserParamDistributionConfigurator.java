/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.metrics.distributions;

import es.uam.eps.socialranksys.diffusion.metrics.distributions.Distribution;
import es.uam.eps.socialranksys.diffusion.metrics.distributions.UserParamDistribution;

import java.io.Serializable;

/**
 * Class for configuring a distribution of the information pieces.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public class UserParamDistributionConfigurator<U extends Serializable,I extends Serializable,P> implements DistributionConfigurator<U,I,P>
{
    /**
     * Identifier for the selected parameter.
     */
    private final static String PARAM = "parameter";
    @Override
    public Distribution<U, I, P> configure(DistributionParamReader params)
    {
        String parameter = params.getParams().getStringValue(PARAM);
        return new UserParamDistribution<>(parameter);
    }
    
}
