/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.links.recommendation.algorithms.standalone.twitter;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.grid.links.recommendation.algorithms.AlgorithmGridSearch;
import es.uam.eps.ir.relison.links.recommendation.algorithms.RecommendationAlgorithmFunction;
import es.uam.eps.ir.relison.links.recommendation.algorithms.standalone.twitter.Money;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.relison.grid.links.recommendation.algorithms.AlgorithmIdentifiers.MONEY;

/**
 * Grid search generator for the Money algorithm.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 *
 * @see Money
 */
public class MoneyGridSearch<U> implements AlgorithmGridSearch<U> 
{   
    /**
     * Identifier for the mode of the algorithm.
     */
    private final static String MODE = "mode";
    
    /**
     * Identifier for the teleport rate for the personalized SALSA algorithm.
     */
    private final static String ALPHA = "alpha";
    /**
     * Identifier for the teleport rate for computing the circle of trust.
     */
    private final static String R = "r";
    /**
     * Identifier for the number of users in the circle of trust.
     */
    private final static String NEIGH = "neigh";

    @Override
    public Map<String, RecommendationAlgorithmFunction<U>> grid(Grid grid)
    {
        Map<String, RecommendationAlgorithmFunction<U>> recs = new HashMap<>();
        
        List<Boolean> modes = grid.getBooleanValues(MODE);
        List<Double> alphas = grid.getDoubleValues(ALPHA);
        List<Double> rs = grid.getDoubleValues(R);
        List<Integer> neighs = grid.getIntegerValues(NEIGH);
        alphas.forEach(alpha ->
            modes.forEach(mode ->
                neighs.forEach(neigh ->
                    rs.forEach(r ->
                        recs.put(MONEY + "_" + (mode ? "auth" : "hubs") + "_" + alpha + "_" + neigh + "_" + r, (graph, prefData) ->
                           new Money<>(graph, neigh, r, mode, alpha)
                        )
                    )
                )
            )
        );
        return recs;
    }

    @Override
    public Map<String, Supplier<Recommender<U, U>>> grid(Grid grid, FastGraph<U> graph, FastPreferenceData<U, U> prefData)
    {
        Map<String, Supplier<Recommender<U,U>>> recs = new HashMap<>();
        
        List<Boolean> modes = grid.getBooleanValues(MODE);
        List<Double> alphas = grid.getDoubleValues(ALPHA);
        List<Double> rs = grid.getDoubleValues(R);
        List<Integer> neighs = grid.getIntegerValues(NEIGH);

        alphas.forEach(alpha ->
            modes.forEach(mode ->
                neighs.forEach(neigh ->
                    rs.forEach(r ->
                        recs.put(MONEY + "_" + (mode ? "auth" : "hubs") + "_" + alpha + "_" + neigh + "_" + r, () ->
                           new Money<>(graph, neigh, r, mode, alpha)
                        )
                    )
                )
            )
        );
        return recs;
    }
    
}
