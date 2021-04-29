/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.community.connectedness;


import es.uam.eps.ir.socialranksys.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.socialranksys.community.detection.connectedness.StronglyConnectedComponents;
import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.community.CommunityDetectionConfigurator;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.community.CommunityDetectionIdentifiers.SCC;

/**
 * Configurator for an algorithm that computes the strongly connected components of a graph.
 * 
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * 
 * @param <U> Type of the nodes of the network.
 * 
 * @see es.uam.eps.ir.socialranksys.community.detection.connectedness.StronglyConnectedComponents
 */
public class StronglyConnectedComponentsConfigurator<U extends Serializable> implements CommunityDetectionConfigurator<U>
{
    @Override
    public Map<String, Supplier<CommunityDetectionAlgorithm<U>>> configure(Grid grid)
    {
        Map<String, Supplier<CommunityDetectionAlgorithm<U>>> map = new HashMap<>();
        map.put(SCC, StronglyConnectedComponents::new);
        return map;
    }
}
