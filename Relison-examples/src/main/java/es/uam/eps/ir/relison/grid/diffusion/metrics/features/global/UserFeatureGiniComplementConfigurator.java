/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.diffusion.metrics.features.global;

import es.uam.eps.ir.relison.diffusion.metrics.SimulationMetric;
import es.uam.eps.ir.relison.diffusion.metrics.features.global.UserFeatureGiniComplement;
import es.uam.eps.ir.relison.grid.Parameters;
import es.uam.eps.ir.relison.grid.diffusion.metrics.MetricConfigurator;

import java.io.Serializable;

/**
 * Interface for the complement of the Gini coefficient over the distribution of (user, feature) pairs.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the features of the user / information pieces.
 *
 * @see UserFeatureGiniComplement
 */
public class UserFeatureGiniComplementConfigurator<U extends Serializable,I extends Serializable, F> implements MetricConfigurator<U,I, F>
{
    /**
     * Identifier for the parameter that indicates if a feature refers to a user or to an information piece.
     */
    private final static String USERFEAT = "userFeature";
    /**
     * Identifier for the name of the feature.
     */
    private final static String PARAM = "parameter";
    /**
     * Identifier for considering unique user-item pairs or not.
     */
    private final static String UNIQUE = "unique";
    
    @Override
    public SimulationMetric<U, I, F> configure(Parameters params)
    {
        Boolean userParam = params.getBooleanValue(USERFEAT);
        String param = params.getStringValue(PARAM);
        Boolean unique = params.getBooleanValue(UNIQUE);
        
        if(userParam == null || param == null || unique == null)
        {
            return null;
        }
        
        return new UserFeatureGiniComplement<>(param, userParam, unique);
    } 
    
}
