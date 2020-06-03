/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.community.modularity.balanced;

import es.uam.eps.ir.socialranksys.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.socialranksys.community.detection.modularity.balanced.GiniWeightedFastGreedy;
import es.uam.eps.ir.socialranksys.grid.Parameters;
import es.uam.eps.ir.socialranksys.grid.community.CommunityDetectionConfigurator;

import java.io.Serializable;

/**
 * Configurator for the balanced version of the FastGreedy community detection algorithm that tries
 * to optimize modularity and Gini of the community sizes.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 * @see es.uam.eps.ir.socialranksys.community.detection.modularity.balanced.GiniWeightedFastGreedy
 */
public class GiniWeightedFastGreedyConfigurator<U extends Serializable> implements CommunityDetectionConfigurator<U>
{
    /**
     * Identifier for the regularization parameter.
     */
    private final static String LAMBDA = "lambda";
    
    @Override
    public CommunityDetectionAlgorithm<U> configure(Parameters params)
    {       
        double lambda = params.getDoubleValue(LAMBDA);
        return new GiniWeightedFastGreedy<>(lambda);
    }
    
}
