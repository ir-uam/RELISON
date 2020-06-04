/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.metrics;

import java.io.Serializable;

/**
 * Interface for the different global metrics (applied over the whole set
 * of users) to apply over the simulation.
 * @author Javier Sanz-Cruzado Puig
 * 
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public interface GlobalSimulationMetric<U extends Serializable,I extends Serializable,P> extends SimulationMetric<U,I,P> 
{
    
}
