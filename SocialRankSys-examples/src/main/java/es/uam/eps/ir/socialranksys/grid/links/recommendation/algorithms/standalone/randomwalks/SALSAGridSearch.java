/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.standalone.randomwalks;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.AlgorithmGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.RecommendationAlgorithmFunction;
import es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.randomwalk.SALSA;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Grid search generator for SALSA algorithm.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 *
 * @see SALSA
 */
public class SALSAGridSearch<U> implements AlgorithmGridSearch<U>
{   
    /**
     * Identifier for the mode of the algorithm.
     */
    private final static String MODE = "mode";

    @Override
    public Map<String, RecommendationAlgorithmFunction<U>> grid(Grid grid)
    {
        Map<String, RecommendationAlgorithmFunction<U>> recs = new HashMap<>();
        
        List<Boolean> modes = grid.getBooleanValues(MODE);
            modes.forEach(mode ->
                recs.put(es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.AlgorithmIdentifiers.SALSA + "_" + (mode ? "auth" : "hubs"), (graph, prefData) -> new SALSA<>(graph, mode)));
        return recs;
    }

    @Override
    public Map<String, Supplier<Recommender<U, U>>> grid(Grid grid, FastGraph<U> graph, FastPreferenceData<U, U> prefData)
    {
        Map<String, Supplier<Recommender<U,U>>> recs = new HashMap<>();
        
        List<Boolean> modes = grid.getBooleanValues(MODE);

        modes.forEach(mode ->
            recs.put(es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.AlgorithmIdentifiers.SALSA + "_" + (mode ? "auth" : "hubs"), () -> new SALSA<>(graph, mode)));
        return recs;
    }
    
}
