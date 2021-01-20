/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.community.modularity;

import es.uam.eps.ir.socialranksys.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.socialranksys.community.detection.modularity.Louvain;
import es.uam.eps.ir.socialranksys.grid.Parameters;
import es.uam.eps.ir.socialranksys.grid.community.CommunityDetectionConfigurator;

import java.io.Serializable;

/**
 * Configurator for the Louvain community detection algorithm.
 * @author Javier Sanz-Cruzado Puig (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @param <U> Type of the users.
 */
public class LouvainConfigurator<U extends Serializable> implements CommunityDetectionConfigurator<U>
{
    /**
     * The parameter identifier for the threshold value.
     */
    private final static String THRESHOLD = "threshold";

    @Override
    public CommunityDetectionAlgorithm<U> configure(Parameters params)
    {
        double threshold = params.getDoubleValue(THRESHOLD);
        return new Louvain<>(threshold);
    }
    
}
