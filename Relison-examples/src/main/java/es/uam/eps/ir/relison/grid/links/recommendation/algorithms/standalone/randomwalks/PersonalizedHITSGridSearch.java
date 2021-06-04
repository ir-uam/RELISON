/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autonoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.links.recommendation.algorithms.standalone.randomwalks;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.grid.links.recommendation.algorithms.AlgorithmGridSearch;
import es.uam.eps.ir.relison.links.recommendation.algorithms.RecommendationAlgorithmFunction;
import es.uam.eps.ir.relison.links.recommendation.algorithms.standalone.randomwalk.PersonalizedHITS;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.relison.grid.links.recommendation.algorithms.AlgorithmIdentifiers.PERSHITS;

/**
 * Grid search generator for the personalized HITS algorithm.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 *
 * @see PersonalizedHITS
 */
public class PersonalizedHITSGridSearch<U> implements AlgorithmGridSearch<U>
{   
    /**
     * Identifier for the mode of the algorithm.
     */
    private final static String MODE = "mode";
    
    /**
     * Teleport rate for the personalized HITS algorithm.
     */
    private final static String ALPHA = "alpha";

    @Override
    public Map<String, RecommendationAlgorithmFunction<U>> grid(Grid grid)
    {
        Map<String, RecommendationAlgorithmFunction<U>> recs = new HashMap<>();
        
        List<Boolean> modes = grid.getBooleanValues(MODE);
        List<Double> alphas = grid.getDoubleValues(ALPHA);
        alphas.forEach(alpha ->
            modes.forEach(mode ->
                recs.put(PERSHITS + "_" + (mode ? "auth" : "hubs") + "_" + alpha, (graph, prefData) -> new PersonalizedHITS<>(graph, mode, alpha))));
        return recs;
    }

    @Override
    public Map<String, Supplier<Recommender<U, U>>> grid(Grid grid, FastGraph<U> graph, FastPreferenceData<U, U> prefData)
    {
        Map<String, Supplier<Recommender<U,U>>> recs = new HashMap<>();
        
        List<Boolean> modes = grid.getBooleanValues(MODE);
        List<Double> alphas = grid.getDoubleValues(ALPHA);

        alphas.forEach(alpha ->
                modes.forEach(mode ->
                        recs.put(PERSHITS + "_" + (mode ? "auth" : "hubs") + "_" + alpha, () -> new PersonalizedHITS<>(graph, mode, alpha))));
        return recs;
    }
    
}
