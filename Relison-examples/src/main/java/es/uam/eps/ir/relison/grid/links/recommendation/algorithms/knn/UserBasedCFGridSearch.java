/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.links.recommendation.algorithms.knn;


import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.nn.sim.Similarity;
import es.uam.eps.ir.ranksys.nn.user.UserNeighborhoodRecommender;
import es.uam.eps.ir.ranksys.nn.user.neighborhood.CachedUserNeighborhood;
import es.uam.eps.ir.ranksys.nn.user.neighborhood.TopKUserNeighborhood;
import es.uam.eps.ir.ranksys.nn.user.neighborhood.UserNeighborhood;
import es.uam.eps.ir.ranksys.nn.user.sim.UserSimilarity;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.ranksys.rec.fast.FastRecommender;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.grid.links.recommendation.algorithms.AlgorithmGridSearch;
import es.uam.eps.ir.relison.grid.links.recommendation.algorithms.AlgorithmGridSelector;
import es.uam.eps.ir.relison.links.recommendation.algorithms.RecommendationAlgorithmFunction;
import es.uam.eps.ir.relison.links.recommendation.algorithms.knn.similarities.RecommenderSimilarity;
import es.uam.eps.ir.relison.links.recommendation.algorithms.knn.similarities.SpecificUserSimilarity;
import org.ranksys.formats.parsing.Parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.relison.grid.links.recommendation.algorithms.AlgorithmIdentifiers.UB;

/**
 * Grid search generator for user-based kNN collaborative filtering algorithm.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
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
    /**
     * Identifier for indicating whether the result is weighted or not.
     */
    private static final String WEIGHTED = "weighted";
    /**
     * A parser for reading users.
     */
    private final Parser<U> uParser;

    public UserBasedCFGridSearch(Parser<U> uParser)
    {
        this.uParser = uParser;
    }

    @Override
    public Map<String, Supplier<Recommender<U, U>>> grid(Grid grid, FastGraph<U> graph, FastPreferenceData<U,U> prefData)
    {
        Map<String, Supplier<Recommender<U,U>>> recs = new HashMap<>();
        
        Map<String, Grid> similarities = grid.getGridValues(SIM);
        List<Integer> ks = grid.getIntegerValues(K);
        List<Integer> qs = grid.getIntegerValues(Q);
        
        AlgorithmGridSelector<U> selector = new AlgorithmGridSelector<>(uParser);
        
        ks.forEach(k ->
            qs.forEach(q ->
                similarities.forEach((simname, simgrid) -> 
                {
                    Map<String, Supplier<Recommender<U,U>>> sims = selector.getRecommenders(simname, simgrid, graph, prefData);
                    sims.forEach((name, sim) ->
                        recs.put(UB + "_" + name + "_" + k + "_" + q, () -> 
                        {
                            Similarity s = new RecommenderSimilarity(graph, (FastRecommender<U,U>) sim.get());
                            UserSimilarity<U> similarity = new SpecificUserSimilarity<>(prefData, s);
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
        List<Boolean> weighted = grid.getBooleanValues(WEIGHTED);
        AlgorithmGridSelector<U> selector = new AlgorithmGridSelector<>(uParser);

        if(weighted.isEmpty())
            ks.forEach(k ->
                qs.forEach(q ->
                    similarities.forEach((simname, simgrid) ->
                    {
                        Map<String, RecommendationAlgorithmFunction<U>> sims = selector.getRecommenders(simname, simgrid);
                        sims.forEach((name, sim) ->
                            recs.put(UB + "_" + name + "_" + k + "_" + q, (FastGraph<U> graph, FastPreferenceData<U,U> prefData) ->
                            {
                                Similarity s = new RecommenderSimilarity(graph, (FastRecommender<U,U>) sim.apply(graph, prefData));
                                UserSimilarity<U> similarity = new SpecificUserSimilarity<>(prefData, s);
                                UserNeighborhood<U> neighborhood = new CachedUserNeighborhood<>(new TopKUserNeighborhood<>(similarity, k));
                                return new UserNeighborhoodRecommender<>(prefData, neighborhood, q);
                            }));
                    })));
        else
            ks.forEach(k ->
                qs.forEach(q ->
                    similarities.forEach((simname, simgrid) ->
                    {
                        Map<String, RecommendationAlgorithmFunction<U>> sims = selector.getRecommenders(simname, simgrid);
                            sims.forEach((name, sim) ->
                                weighted.forEach( weight ->
                                    recs.put(UB + "_" + (weight ? "wei" : "unw") + "_" + name + "_" + k + "_" + q, new RecommendationAlgorithmFunction<>()
                                    {
                                        @Override
                                        public Recommender<U, U> apply(FastGraph<U> graph, FastPreferenceData<U, U> prefData)
                                        {
                                            Similarity s = new RecommenderSimilarity(graph, (FastRecommender<U,U>) sim.apply(graph, prefData));
                                            UserSimilarity<U> similarity = new SpecificUserSimilarity<>(prefData, s);
                                            UserNeighborhood<U> neighborhood = new CachedUserNeighborhood<>(new TopKUserNeighborhood<>(similarity, k));
                                            return new UserNeighborhoodRecommender<>(prefData, neighborhood, q);
                                        }

                                        @Override
                                        public boolean isWeighted()
                                        {
                                            return weight;
                                        }
                                    })
                                )
                            );
                    })
                )
            );
        return recs;
    }
}
