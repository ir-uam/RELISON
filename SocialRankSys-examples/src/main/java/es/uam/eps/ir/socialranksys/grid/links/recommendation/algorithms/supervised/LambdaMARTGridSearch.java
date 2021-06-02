/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.supervised;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.AlgorithmGridSearch;
import es.uam.eps.ir.socialranksys.links.recommendation.algorithms.RecommendationAlgorithmFunction;
import es.uam.eps.ir.socialranksys.links.recommendation.algorithms.supervised.LambdaMARTJForestsRecommender;
import org.ranksys.formats.parsing.Parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.AlgorithmIdentifiers.LAMBDAMART;


/**
 * Grid search generator for LambdaMART algorithm. Differently from other algorithms, for the LambdaMART
 * algorithm, we consider the grid to be a list of configurations.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see es.uam.eps.ir.socialranksys.links.recommendation.algorithms.supervised.LambdaMARTJForestsRecommender
 */
public class LambdaMARTGridSearch<U> implements AlgorithmGridSearch<U>
{
    /**
     * Identifier for the route containing the training instances.
     */
    private final static String TRAIN = "train";
    /**
     * Identifier for the route containing the test instances.
     */
    private final static String TEST = "test";
    /**
     * Identifier for the route containing the validation instances.
     */
    private final static String VALID = "valid";
    /**
     * Identifier for the route of the jforests configuration file.
     */
    private final static String CONFIG = "config";
    /**
     * Identifier for the route of the temporary folder.
     */
    private final static String TMP = "tmp";

    /**
     * Parser for reading users.
     */
    private final Parser<U> uParser;

    /**
     * Constructor.
     * @param uParser a parser for reading users.
     */
    public LambdaMARTGridSearch(Parser<U> uParser)
    {
        this.uParser = uParser;
    }

    @Override
    public Map<String, RecommendationAlgorithmFunction<U>> grid(Grid grid)
    {
        Map<String, RecommendationAlgorithmFunction<U>> recs = new HashMap<>();

        List<String> trainFiles = grid.getStringValues(TRAIN);
        List<String> testFiles = grid.getStringValues(TEST);
        List<String> validFiles = grid.getStringValues(VALID);
        List<String> configFiles = grid.getStringValues(CONFIG);
        List<String> tmpDirs = grid.getStringValues(TMP);

        int maxI = Math.min(Math.min(Math.min(trainFiles.size(), testFiles.size()), Math.min(validFiles.size(), configFiles.size())),tmpDirs.size());
        for(int i = 0; i < maxI; ++i)
        {
            int j = i;
            recs.put(LAMBDAMART + "_" + i, (graph, prefData) ->
            {
                try
                {
                    return new LambdaMARTJForestsRecommender<>(graph, trainFiles.get(j), validFiles.get(j), testFiles.get(j), configFiles.get(j), tmpDirs.get(j), uParser);
                }
                catch (Exception e)
                {
                    return null;
                }
            });
        }

        return recs;
    }

    @Override
    public Map<String, Supplier<Recommender<U, U>>> grid(Grid grid, FastGraph<U> graph, FastPreferenceData<U, U> prefData)
    {
        Map<String, Supplier<Recommender<U,U>>> recs = new HashMap<>();

        List<String> trainFiles = grid.getStringValues(TRAIN);
        List<String> testFiles = grid.getStringValues(TEST);
        List<String> validFiles = grid.getStringValues(VALID);
        List<String> configFiles = grid.getStringValues(CONFIG);
        List<String> tmpDirs = grid.getStringValues(TMP);

        int maxI = Math.min(Math.min(Math.min(trainFiles.size(), testFiles.size()), Math.min(validFiles.size(), configFiles.size())),tmpDirs.size());
        for(int i = 0; i < maxI; ++i)
        {
            int j = i;
            recs.put(LAMBDAMART + "_" + i, () ->
            {
                try
                {
                    return new LambdaMARTJForestsRecommender<>(graph, trainFiles.get(j), validFiles.get(j), testFiles.get(j), configFiles.get(j), tmpDirs.get(j), uParser);
                }
                catch (Exception e)
                {
                    return null;
                }
            });
        }

        return recs;
    }

}
