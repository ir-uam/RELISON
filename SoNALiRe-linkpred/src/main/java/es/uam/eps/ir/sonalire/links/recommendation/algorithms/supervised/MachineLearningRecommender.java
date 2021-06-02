/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.algorithms.supervised;

import es.uam.eps.ir.sonalire.graph.fast.FastGraph;
import es.uam.eps.ir.sonalire.links.data.letor.InstanceSet;
import es.uam.eps.ir.sonalire.links.data.ml.MLConstants;
import es.uam.eps.ir.sonalire.links.linkprediction.supervised.Classifier;
import es.uam.eps.ir.sonalire.links.recommendation.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

import java.util.Map;


/**
 * Contact recommendation algorithm that uses supervised classification techniques to generate the
 * recommendation.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class MachineLearningRecommender<U> extends UserFastRankingRecommender<U>
{
    /**
     * Classifier.
     */
    private final Classifier<U> classifier;
    /**
     * Test instances: contains the instances for the links we want to recommend.
     */
    private final InstanceSet<U> testSet;

    /**
     * Constructor
     * @param graph         the training graph to consider.
     * @param classifier    the classifier.
     * @param trainSet      the set of instances in the training set.
     * @param testSet       the set of instances in the test set.
     */
    public MachineLearningRecommender(FastGraph<U> graph, Classifier<U> classifier, InstanceSet<U> trainSet, InstanceSet<U> testSet)
    {
        super(graph);
        
        this.classifier = classifier;
        this.testSet = testSet;
        try
        {
            classifier.train(trainSet);
        } 
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public Int2DoubleMap getScoresMap(int uidx) 
    {
        Int2DoubleMap scores = new Int2DoubleOpenHashMap();
        scores.defaultReturnValue(Double.NEGATIVE_INFINITY);
        
        U u = this.uidx2user(uidx);
        
        this.testSet.getAllInstancesWithOrigin(u).forEach(pat -> 
        {
            Map<Integer, Double> classScores = classifier.computeScores(pat);
            double score = classScores.get(MLConstants.POSITIVECLASS);
            scores.put(this.item2iidx(pat.getDest()), score);
        });
                
        return scores;
    }
}
