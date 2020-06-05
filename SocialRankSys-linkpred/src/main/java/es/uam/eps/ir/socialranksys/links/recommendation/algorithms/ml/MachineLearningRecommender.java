/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.ml;

import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.data.letor.InstanceSet;
import es.uam.eps.ir.socialranksys.links.linkprediction.supervised.Classifier;
import es.uam.eps.ir.socialranksys.links.recommendation.UserFastRankingRecommender;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import org.openide.util.Exceptions;
import weka.core.FastVector;

import java.util.Map;

import static es.uam.eps.ir.socialranksys.links.data.ml.MLConstants.POSITIVECLASS;


/**
 *
 * @author Javier
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
     * @param graph Graph
     * @param classifier Weka classifier
     * @param trainSet Train set
     * @param testSet Test set
     * @param relation relation between pair of users and instances.
     * @param attributes List of attributes and possible values for nominal attributes.
     */
    public MachineLearningRecommender(FastGraph<U> graph, Classifier<U> classifier, InstanceSet<U> trainSet, InstanceSet<U> testSet, Map<Pair<U>,Integer> relation, FastVector attributes)
    {
        super(graph);
        
        this.classifier = classifier;
        this.testSet = testSet;
        try
        {
            classifier.train(trainSet);
        } 
        catch (Exception ex) {
            Exceptions.printStackTrace(ex);
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
            double score = classScores.get(POSITIVECLASS);
            scores.put(this.item2iidx(pat.getDest()), score);
        });
                
        return scores;
    }
}
