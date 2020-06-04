/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.metrics.distributions;

import es.uam.eps.ir.socialranksys.diffusion.metrics.distributions.Distribution;
import es.uam.eps.ir.socialranksys.utils.datatypes.Tuple2oo;

import java.io.Serializable;
import java.util.List;

import static es.uam.eps.ir.socialranksys.grid.diffusion.metrics.distributions.DistributionIdentifiers.*;

/**
 * Class that selects an individual distribution.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 * @see es.uam.eps.ir.socialranksys.diffusion.metrics.distributions
 */
public class DistributionSelector<U extends Serializable,I extends Serializable,P> 
{
    /**
     * Selects and configures a distribution.
     * @param ppr Parameters for the distribution.
     * @return A pair containing the name and the selected distribution.
     */
    public Tuple2oo<String, Tuple2oo<Distribution<U,I,P>, List<Integer>>> select(DistributionParamReader ppr)
    {
        String name = ppr.getName();
        DistributionConfigurator<U,I,P> conf;
        switch(name)
        {
            case INFOPARAM:
                conf = new InfoParamDistributionConfigurator<>();
                break;
            case USERPARAM:
                conf = new UserParamDistributionConfigurator<>();
                break;
            case INFORMATION:
                conf = new InfoPiecesDistributionConfigurator<>();
                break;
            case MIXEDPARAM:
                conf = new MixedParamDistributionConfigurator<>();
                break;
            default:
                return null;
        }
        
        Distribution<U,I,P> propagation = conf.configure(ppr);
        return new Tuple2oo<>(propagation.getName(), new Tuple2oo<>(propagation, ppr.getTimes()));
    }
}
