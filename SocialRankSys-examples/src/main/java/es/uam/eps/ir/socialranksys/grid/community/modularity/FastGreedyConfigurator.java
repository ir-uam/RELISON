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
import es.uam.eps.ir.socialranksys.community.detection.modularity.FastGreedy;
import es.uam.eps.ir.socialranksys.grid.Parameters;
import es.uam.eps.ir.socialranksys.grid.community.CommunityDetectionConfigurator;

import java.io.Serializable;

/**
 * Configurator for the FastGreedy community detection algorithm
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 * @see es.uam.eps.ir.socialranksys.community.detection.modularity.FastGreedy
 */
public class FastGreedyConfigurator<U extends Serializable> implements CommunityDetectionConfigurator<U>
{ 
    @Override
    public CommunityDetectionAlgorithm<U> configure(Parameters params)
    {
        return new FastGreedy<>();
    }
    
}
