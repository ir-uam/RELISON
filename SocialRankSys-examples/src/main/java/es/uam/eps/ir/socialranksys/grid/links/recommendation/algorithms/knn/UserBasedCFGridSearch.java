/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.knn;


import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.nn.sim.Similarity;
import es.uam.eps.ir.ranksys.nn.user.UserNeighborhoodRecommender;
import es.uam.eps.ir.ranksys.nn.user.neighborhood.CachedUserNeighborhood;
import es.uam.eps.ir.ranksys.nn.user.neighborhood.TopKUserNeighborhood;
import es.uam.eps.ir.ranksys.nn.user.neighborhood.UserNeighborhood;
import es.uam.eps.ir.ranksys.nn.user.sim.UserSimilarity;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.AlgorithmGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.RecommendationAlgorithmFunction;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.knn.similarities.SimilarityFunction;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.knn.similarities.SimilarityGridSelector;
import es.uam.eps.ir.socialranksys.links.recommendation.knn.similarities.SpecificUserSimilarity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.AlgorithmIdentifiers.UB;

/**
 * Grid search generator for User Based CF algorithm.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public class UserBasedCFGridSearch<U> implements AlgorithmGridSearch<U>
{   
    /**
     * Identifier for the number of neighbors of the algorithm.
     */
    private final static String K = "k";
    /**
     * Identifier for the similarity.
     */
    private final static String SIM = "sim";
    /**
     * Exponent of the similarity.
     */
    private final static String Q = "q";
    
    @Override
    public Map<String, Supplier<Recommender<U, U>>> grid(Grid grid, FastGraph<U> graph, FastPreferenceData<U,U> prefData)
    {
        Map<String, Supplier<Recommender<U,U>>> recs = new HashMap<>();
        
        Map<String, Grid> similarities = grid.getGridValues(SIM);
        List<Integer> ks = grid.getIntegerValues(K);
        List<Integer> qs = grid.getIntegerValues(Q);
        
        SimilarityGridSelector<U> selector = new SimilarityGridSelector<>();
        
        ks.forEach(k ->
            qs.forEach(q ->
                similarities.forEach((simname, simgrid) -> 
                {
                    Map<String, Supplier<Similarity>> sims = selector.getSimilarities(simname, simgrid, graph, prefData);
                    sims.forEach((name, sim) ->
                        recs.put(UB + "_" + name + "_" + k + "_" + q, () -> 
                        {
                            UserSimilarity<U> similarity = new SpecificUserSimilarity<>(prefData, sim.get());
                            UserNeighborhood<U> neighborhood = new CachedUserNeighborhood<>(new TopKUserNeighborhood<>(similarity, k));
                            return new UserNeighborhoodRecommender<>(prefData, neighborhood, q);
                        }));
                })));
        return recs;
    }

    @Override
    public Map<String, RecommendationAlgorithmFunction<U>> grid(Grid grid)
    {
        Map<String, RecommendationAlgorithmFunction<U>> recs = new HashMap<>();
        
        Map<String, Grid> similarities = grid.getGridValues(SIM);
        List<Integer> ks = grid.getIntegerValues(K);
        List<Integer> qs = grid.getIntegerValues(Q);
        
        SimilarityGridSelector<U> selector = new SimilarityGridSelector<>();

        ks.forEach(k ->
            qs.forEach(q ->
                similarities.forEach((simname, simgrid) ->
                {
                    Map<String, SimilarityFunction<U>> sims = selector.getSimilarities(simname, simgrid);
                    sims.forEach((name, sim) ->
                        recs.put(UB + "_" + name + "_" + k + "_" + q, (FastGraph<U> graph, FastPreferenceData<U,U> prefData) ->
                        {
                            UserSimilarity<U> similarity = new SpecificUserSimilarity<>(prefData, sim.apply(graph, prefData));
                            UserNeighborhood<U> neighborhood = new CachedUserNeighborhood<>(new TopKUserNeighborhood<>(similarity, k));
                            return new UserNeighborhoodRecommender<>(prefData, neighborhood, q);
                        }));
                })));
        return recs;
    }
}
