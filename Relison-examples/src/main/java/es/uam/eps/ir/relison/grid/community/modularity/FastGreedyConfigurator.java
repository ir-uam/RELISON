/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.community.modularity;


import es.uam.eps.ir.relison.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.relison.community.detection.modularity.FastGreedy;
import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.grid.community.CommunityDetectionConfigurator;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.relison.grid.community.CommunityDetectionIdentifiers.FASTGREEDY;

/**
 * Configures the FastGreedy community detection algorithm.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 *
 * @see FastGreedy
 */
public class FastGreedyConfigurator<U extends Serializable> implements CommunityDetectionConfigurator<U>
{
    @Override
    public Map<String, Supplier<CommunityDetectionAlgorithm<U>>> configure(Grid grid)
    {
        Map<String, Supplier<CommunityDetectionAlgorithm<U>>> map = new HashMap<>();
        map.put(FASTGREEDY, FastGreedy::new);
        return map;
    }
}
