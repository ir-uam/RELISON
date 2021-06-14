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

import java.io.Serializable;

/**
 * Interface for configuring a distribution on an information diffusion process.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> Type of the user / information pieces features.
 */
public interface DistributionConfigurator<U extends Serializable,I extends Serializable, F>
{
    /**
     * Configures a information propagation distribution.
     * @param params the parameters of the distribution.
     * @return the distribution
     */
    Distribution<U,I,F> configure(Parameters params);
}
