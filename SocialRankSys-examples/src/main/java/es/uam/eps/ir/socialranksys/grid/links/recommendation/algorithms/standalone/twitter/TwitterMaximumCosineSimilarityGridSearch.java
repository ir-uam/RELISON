/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.standalone.twitter;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.AlgorithmGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.RecommendationAlgorithmFunction;
import es.uam.eps.ir.socialranksys.links.recommendation.standalone.twitter.TwitterMaximumCosineSimilarity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.AlgorithmIdentifiers.TWITTERMAXCOS;

/**
 * Grid search generator for Twitter version of the Maximum Cosine Similarity algorithm.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public class TwitterMaximumCosineSimilarityGridSearch<U> implements AlgorithmGridSearch<U> 
{   
    /**
     * Identifier for the teleport rate for computing the circle of trust
     */
    private final static String R = "r";
    /**
     * Identifier for the number of users in the circle of trust
     */
    private final static String NEIGH = "neigh";

    @Override
    public Map<String, RecommendationAlgorithmFunction<U>> grid(Grid grid)
    {
        Map<String, RecommendationAlgorithmFunction<U>> recs = new HashMap<>();

        List<Double> rs = grid.getDoubleValues(R);
        List<Integer> neighs = grid.getIntegerValues(NEIGH);
        
        rs.forEach(r ->
            neighs.forEach(neigh ->
                recs.put(TWITTERMAXCOS + "_" + neigh + "_" + r, (graph, prefData) ->
                    new TwitterMaximumCosineSimilarity<>(graph, neigh, r)
                )
            )
        );
        
        return recs;
    }

    @Override
    public Map<String, Supplier<Recommender<U, U>>> grid(Grid grid, FastGraph<U> graph, FastPreferenceData<U, U> prefData)
    {
        Map<String, Supplier<Recommender<U,U>>> recs = new HashMap<>();

        List<Double> rs = grid.getDoubleValues(R);
        List<Integer> neighs = grid.getIntegerValues(NEIGH);
        
        rs.forEach(r ->
            neighs.forEach(neigh ->
                recs.put(TWITTERMAXCOS + "_" + neigh + "_" + r, () ->
                   new TwitterMaximumCosineSimilarity<>(graph, neigh, r)
                )
            )
        );
        
        return recs;
    }
    
}
