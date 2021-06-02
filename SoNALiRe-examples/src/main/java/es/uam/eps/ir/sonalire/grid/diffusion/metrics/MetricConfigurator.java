/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.diffusion.metrics;

import es.uam.eps.ir.sonalire.diffusion.metrics.SimulationMetric;
import es.uam.eps.ir.sonalire.grid.Parameters;

import java.io.Serializable;

/**
 * Interface for the configuration of an information diffusion metric.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the features of the user / information pieces.
 */
public interface MetricConfigurator<U extends Serializable,I extends Serializable, F>
{
    /**
     * Configures a information propagation metric.
     * @param params the parameters of the metric.
     * @return the diffusion metric.
     */
    SimulationMetric<U,I, F> configure(Parameters params);
}
