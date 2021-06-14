/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.links.recommendation.algorithms.standalone.mf;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.mf.Factorization;
import es.uam.eps.ir.ranksys.mf.Factorizer;
import es.uam.eps.ir.ranksys.mf.als.HKVFactorizer;
import es.uam.eps.ir.ranksys.mf.rec.MFRecommender;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.grid.links.recommendation.algorithms.AlgorithmGridSearch;
import es.uam.eps.ir.relison.links.recommendation.algorithms.RecommendationAlgorithmFunction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Supplier;

import static es.uam.eps.ir.relison.grid.links.recommendation.algorithms.AlgorithmIdentifiers.IMF;

/**
 * Grid search generator for the Implicit Matrix Factorization algorithm by 
 * Hu, Koren and Volinsky (HKV) algorithm.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 *
 * @see es.uam.eps.ir.ranksys.mf.als.HKVFactorizer
 * @see es.uam.eps.ir.ranksys.mf.rec.MFRecommender
 */
public class ImplicitMFGridSearch<U> implements AlgorithmGridSearch<U>
{   
    /**
     * Identifier fort the parameter that regulates the importance of the error and the norm of the latent vectors.
     */
    private final static String LAMBDA = "lambda";
    /**
     * Identifier for the rate of increase for the confidence
     */
    private final static String ALPHA = "alpha";
    /**
     * Identifier for the number of latent factors.
     */
    private final static String K = "k";
    /**
     * Number of iterations for the algorithm
     */
    private final static int NUMITER = 20;
    /**
     * Identifier for indicating whether the result is weighted or not.
     */
    private static final String WEIGHTED = "weighted";

    @Override
    public Map<String, RecommendationAlgorithmFunction<U>> grid(Grid grid)
    {
        Map<String, RecommendationAlgorithmFunction<U>> recs = new HashMap<>();
        
        List<Double> lambdas = grid.getDoubleValues(LAMBDA);
        List<Double> alphas = grid.getDoubleValues(ALPHA);
        List<Integer> ks = grid.getIntegerValues(K);
        List<Boolean> weighted = grid.getBooleanValues(WEIGHTED);

        if(weighted.isEmpty())
            alphas.forEach(alpha ->
            {
                DoubleUnaryOperator confidence = (double x) -> 1 + alpha*x;
                ks.forEach(k ->
                    lambdas.forEach(lambda ->
                        recs.put(IMF + "_" + k + "_" + lambda + "_" + alpha, (graph, prefData) ->
                        {
                           Factorizer<U, U> factorizer = new HKVFactorizer<>(lambda, confidence, NUMITER);
                           Factorization<U, U> factorization = factorizer.factorize(k, prefData);
                           return new MFRecommender<>(prefData, prefData, factorization);
                        })));
            });
        else
            alphas.forEach(alpha ->
            {
                DoubleUnaryOperator confidence = (double x) -> 1 + alpha*x;
                ks.forEach(k ->
                    lambdas.forEach(lambda ->
                        weighted.forEach(weight ->
                            recs.put(IMF + "_" + (weight ? "wei" : "unw") + "_" + k + "_" + lambda + "_" + alpha, new RecommendationAlgorithmFunction<>()
                            {
                                @Override
                                public Recommender<U, U> apply(FastGraph<U> graph, FastPreferenceData<U, U> prefData)
                                {
                                    Factorizer<U, U> factorizer = new HKVFactorizer<>(lambda, confidence, NUMITER);
                                    Factorization<U, U> factorization = factorizer.factorize(k, prefData);
                                    return new MFRecommender<>(prefData, prefData, factorization);
                                }

                                @Override
                                public boolean isWeighted()
                                {
                                    return weight;
                                }
                            }
                            ))));
            });
        return recs;
    }

    @Override
    public Map<String, Supplier<Recommender<U, U>>> grid(Grid grid, FastGraph<U> graph, FastPreferenceData<U, U> prefData)
    {
        Map<String, Supplier<Recommender<U,U>>> recs = new HashMap<>();
        
        List<Double> lambdas = grid.getDoubleValues(LAMBDA);
        List<Double> alphas = grid.getDoubleValues(ALPHA);
        List<Integer> ks = grid.getIntegerValues(K);

        alphas.forEach(alpha ->
        {
            DoubleUnaryOperator confidence = (double x) -> 1 + alpha*x;
            ks.forEach(k ->
                lambdas.forEach(lambda ->
                    recs.put(IMF + "_" + k + "_" + lambda + "_" + alpha, () ->
                    {
                        Factorizer<U, U> factorizer = new HKVFactorizer<>(lambda, confidence, NUMITER);
                        Factorization<U, U> factorization = factorizer.factorize(k, prefData);
                        return new MFRecommender<>(prefData, prefData, factorization);
                    })));
        });
        return recs;
    }
    
}
