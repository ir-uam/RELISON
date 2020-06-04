/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.sight;

import es.uam.eps.socialranksys.diffusion.sight.RecommendedSightMechanism;
import es.uam.eps.socialranksys.diffusion.sight.SightMechanism;

import java.io.Serializable;

/**
 * Configures a Recommended sight mechanism.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 * 
 * @see es.uam.eps.socialranksys.diffusion.sight.RecommendedSightMechanism
 */
public class RecommendedSightConfigurator<U extends Serializable, I extends Serializable, P> implements SightConfigurator<U,I,P> {

    /**
     * Identifier for the probability of observing pieces of information from the recommended links
     */
    private final static String PROBREC = "probRec";
    /**
     * Identifier for the probability of observing pieces of information from training links.
     */
    private final static String PROBTRAIN = "probTrain";
    
    @Override
    public SightMechanism<U, I, P> configure(SightParamReader params)
    {
        double probRec = params.getParams().getDoubleValue(PROBREC);
        double probTrain = params.getParams().getDoubleValue(PROBTRAIN);
        
        return new RecommendedSightMechanism<>(probRec, probTrain);
    }
    
}
