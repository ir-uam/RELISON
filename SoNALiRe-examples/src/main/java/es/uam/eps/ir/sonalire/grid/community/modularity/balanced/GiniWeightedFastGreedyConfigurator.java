/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.community.modularity.balanced;

import es.uam.eps.ir.sonalire.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.sonalire.community.detection.modularity.balanced.GiniWeightedFastGreedy;
import es.uam.eps.ir.sonalire.grid.Grid;
import es.uam.eps.ir.sonalire.grid.community.CommunityDetectionConfigurator;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.sonalire.grid.community.CommunityDetectionIdentifiers.GINIWEIGHTEDFASTGREEDY;

/**
 * Configurator for the balanced version of the FastGreedy community detection algorithm that tries
 * to optimize modularity and Gini of the community sizes.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 * @see GiniWeightedFastGreedy
 */
public class GiniWeightedFastGreedyConfigurator<U extends Serializable> implements CommunityDetectionConfigurator<U>
{
    /**
     * Identifier for the regularization parameter.
     */
    private final static String LAMBDA = "lambda";

    @Override
    public Map<String, Supplier<CommunityDetectionAlgorithm<U>>> configure(Grid grid)
    {
        Map<String, Supplier<CommunityDetectionAlgorithm<U>>> map = new HashMap<>();
        List<Double> lambdas = grid.getDoubleValues(LAMBDA);
        lambdas.forEach(lambda -> map.put(GINIWEIGHTEDFASTGREEDY + "_" + lambda, () -> new GiniWeightedFastGreedy<>(lambda)));
        return map;
    }
}
