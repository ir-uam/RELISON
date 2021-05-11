/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.metrics.users;

import es.uam.eps.ir.socialranksys.diffusion.metrics.SimulationMetric;
import es.uam.eps.ir.socialranksys.diffusion.metrics.user.global.UserGlobalEntropy;
import es.uam.eps.ir.socialranksys.diffusion.metrics.user.individual.UserRecall;
import es.uam.eps.ir.socialranksys.grid.Parameters;
import es.uam.eps.ir.socialranksys.grid.diffusion.metrics.MetricConfigurator;

import java.io.Serializable;

/**
 * Configures a metric that measures the fraction of people discovered by each user in the network.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user / information features.
 *
 * @see UserGlobalEntropy
 */
public class UserRecallMetricConfigurator<U extends Serializable,I extends Serializable, F> implements MetricConfigurator<U,I, F>
{
    @Override
    public SimulationMetric<U, I, F> configure(Parameters params)
    {
        return new UserRecall<>();
    }
    
}
