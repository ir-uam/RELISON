/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.community.modularity.balanced;

import es.uam.eps.ir.relison.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.relison.community.detection.modularity.balanced.BalancedFastGreedy;
import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.grid.community.CommunityDetectionConfigurator;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.relison.grid.community.CommunityDetectionIdentifiers.BALANCEDFASTGREEDY;

/**
 * Configurator for the balanced version of the FastGreedy community detection algorithm.
 * It fixes the maximum community size.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 *
 * @see BalancedFastGreedy
 */
public class BalancedFastGreedyConfigurator<U extends Serializable> implements CommunityDetectionConfigurator<U>
{
    /**
     * Identifier for the maximum community size
     */
    private final static String COMMSIZE = "size";

    @Override
    public Map<String, Supplier<CommunityDetectionAlgorithm<U>>> configure(Grid grid)
    {
        Map<String, Supplier<CommunityDetectionAlgorithm<U>>> map = new HashMap<>();
        List<Integer> commSizes = grid.getIntegerValues(COMMSIZE);
        commSizes.forEach(size -> map.put(BALANCEDFASTGREEDY, () -> new BalancedFastGreedy<>(size)));
        return map;
    }
}
