/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.supervised;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.sonalire.graph.fast.FastGraph;
import es.uam.eps.ir.sonalire.grid.Grid;
import es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.AlgorithmGridSearch;
import es.uam.eps.ir.sonalire.links.recommendation.algorithms.RecommendationAlgorithmFunction;
import es.uam.eps.ir.sonalire.links.recommendation.algorithms.supervised.MachineLearningWekaRecommender;
import org.jooq.lambda.tuple.Tuple2;
import org.ranksys.formats.parsing.Parser;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.Logistic;
import weka.classifiers.meta.AdaBoostM1;
import weka.classifiers.trees.DecisionStump;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Grid search generator for LambdaMART algorithm. Differently from other algorithms, for the LambdaMART
 * algorithm, we consider the grid to be a list of configurations.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see MachineLearningWekaRecommender
 */
public class WekaMLGridSearch<U> implements AlgorithmGridSearch<U>
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
     * Identifier for the route of the jforests configuration file.
     */
    private final static String CLASSIFIER = "classifier";

    /**
     * The number of iterations of the random forest algorithm.
     */
    private final static String ITER = "iterations";

    /**
     * Parser for reading users.
     */
    private final Parser<U> uParser;

    /**
     * Constructor.
     * @param uParser a parser for reading users.
     */
    public WekaMLGridSearch(Parser<U> uParser)
    {
        this.uParser = uParser;
    }

    @Override
    public Map<String, RecommendationAlgorithmFunction<U>> grid(Grid grid)
    {
        Map<String, RecommendationAlgorithmFunction<U>> recs = new HashMap<>();

        List<String> trainFiles = grid.getStringValues(TRAIN);
        List<String> testFiles = grid.getStringValues(TEST);
        Map<String, Grid> classifiersGrid = grid.getGridValues(CLASSIFIER);

        // As a first step, we obtain the corresponding classifiers:
        List<Tuple2<String, Supplier<Classifier>>> classifiers = this.selectClassifiers(classifiersGrid);


        int maxI = Math.min(trainFiles.size(), testFiles.size());

        classifiers.forEach(classifier ->
        {
            for(int i = 0; i < maxI; ++i)
            {
                int j = i;
                recs.put(classifier.v1 + "_" + i, (graph, prefData) ->
                {
                    try
                    {
                        return new MachineLearningWekaRecommender<>(graph, classifier.v2.get(), trainFiles.get(j), testFiles.get(j), uParser);
                    }
                    catch (Exception e)
                    {
                        return null;
                    }
                });
            }
        });

        return recs;
    }

    private List<Tuple2<String, Supplier<Classifier>>> selectClassifiers(Map<String, Grid> classifiersGrid)
    {
        List<Tuple2<String, Supplier<Classifier>>> list = new ArrayList<>();

        classifiersGrid.forEach((name, grid) ->
        {
            switch (name)
            {
                case "naive-bayes":
                    list.add(new Tuple2<>(name, NaiveBayes::new));
                    break;
                case "logistic":
                    list.add(new Tuple2<>(name, Logistic::new));
                    break;
                case "random-forest":
                    List<Integer> iters = grid.getIntegerValues(ITER);
                    iters.forEach(iter -> list.add(new Tuple2<>(name + "_" + iter, () ->
                    {
                        AdaBoostM1 ada = new AdaBoostM1();
                        ada.setClassifier(new DecisionStump());
                        ada.setNumIterations(iter);
                        return ada;
                    })));
                    break;
                default:
                    break;
            }
        });

        return list;
    }

    @Override
    public Map<String, Supplier<Recommender<U, U>>> grid(Grid grid, FastGraph<U> graph, FastPreferenceData<U, U> prefData)
    {
        Map<String, Supplier<Recommender<U,U>>> recs = new HashMap<>();

        List<String> trainFiles = grid.getStringValues(TRAIN);
        List<String> testFiles = grid.getStringValues(TEST);
        Map<String, Grid> classifiersGrid = grid.getGridValues(CLASSIFIER);

        // As a first step, we obtain the corresponding classifiers:
        List<Tuple2<String, Supplier<Classifier>>> classifiers = this.selectClassifiers(classifiersGrid);


        int maxI = Math.min(trainFiles.size(), testFiles.size());

        classifiers.forEach(classifier ->
        {
            for(int i = 0; i < maxI; ++i)
            {
                int j = i;
                recs.put(classifier.v1 + "_" + i, () ->
                {
                    try
                    {
                        return new MachineLearningWekaRecommender<>(graph, classifier.v2.get(), trainFiles.get(j), testFiles.get(j), uParser);
                    }
                    catch (Exception e)
                    {
                        return null;
                    }
                });
            }
        });

        return recs;
    }

}
