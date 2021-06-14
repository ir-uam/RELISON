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
import es.uam.eps.ir.relison.grid.Parameters;
import org.jooq.lambda.tuple.Tuple3;

import java.io.Serializable;
import java.util.List;

import static es.uam.eps.ir.relison.grid.diffusion.metrics.distributions.DistributionIdentifiers.*;

/**
 * Class for selecting a distribution.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> Type of the user / information pieces features.
 */
public class DistributionSelector<U extends Serializable,I extends Serializable, F>
{
    /**
     * Selects and configures a distribution.
     * @param name      the name of the distribution.
     * @param params    the parameters for the distribution.
     * @param times     a list of the times when the distribution has to be executed.
     * @return A triplet containing the name, the distribution and the times.
     */
    public Tuple3<String, Distribution<U,I, F>, List<Integer>> select(String name, Parameters params, List<Integer> times)
    {
        DistributionConfigurator<U,I, F> conf = switch (name)
        {
            case INFOFEATS -> new InfoFeatureDistributionConfigurator<>();
            case USERFEATS -> new UserFeatDistributionConfigurator<>();
            case INFORMATION -> new InfoPiecesDistributionConfigurator<>();
            case MIXEDFEATS -> new MixedParamDistributionConfigurator<>();
            case USERS -> new UserDistributionConfigurator<>();
            default -> null;
        };

        if(conf == null) return null;

        Distribution<U,I, F> propagation = conf.configure(params);
        return new Tuple3<>(propagation.getName(), propagation, times);
    }
}
