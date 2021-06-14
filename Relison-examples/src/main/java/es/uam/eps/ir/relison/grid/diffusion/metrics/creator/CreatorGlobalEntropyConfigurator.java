/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.diffusion.metrics.creator;

import es.uam.eps.ir.relison.diffusion.metrics.SimulationMetric;
import es.uam.eps.ir.relison.diffusion.metrics.creator.global.CreatorGlobalEntropy;
import es.uam.eps.ir.relison.grid.Parameters;
import es.uam.eps.ir.relison.grid.diffusion.metrics.MetricConfigurator;

import java.io.Serializable;

/**
 * Configures a metric that measures the entropy over the set of creators whose information has been received by each user
 * in the network.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user / information features.
 *
 * @see CreatorGlobalEntropy
 */
public class CreatorGlobalEntropyConfigurator<U extends Serializable,I extends Serializable, F> implements MetricConfigurator<U,I, F>
{
    /**
     * Identifier for considering unique user-information piece pairs or not.
     */
    private final static String UNIQUE = "unique";
    
    @Override
    public SimulationMetric<U, I, F> configure(Parameters params)
    {
        Boolean unique = params.getBooleanValue(UNIQUE);
        
        if(unique == null)
        {
            return null;
        }
        
        return new CreatorGlobalEntropy<>(unique);
    }
}
