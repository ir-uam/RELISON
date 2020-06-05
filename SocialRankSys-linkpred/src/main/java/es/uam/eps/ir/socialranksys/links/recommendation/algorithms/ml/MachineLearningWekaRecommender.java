/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.ml;

import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.UserFastRankingRecommender;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import org.openide.util.Exceptions;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import java.util.Map;

import static es.uam.eps.ir.socialranksys.links.data.ml.MLConstants.POSITIVECLASS;


/**
 * Machine learning technique-based recommender which uses the Weka library.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public class MachineLearningWekaRecommender<U> extends UserFastRankingRecommender<U>
{

    /**
     * Weka classifier.
     */
    private final weka.classifiers.Classifier classifier;
    /**
     * Test instances: contains the instances for the links we want to recommend.
     */
    private final Instances testSet;
    /**
     * Relation between test instances and true edges
     */
    private final Map<Pair<U>, Integer> relation;
    /**
     * The list of attributes.
     */
    private final FastVector attributes;
    
    /**
     * Constructor
     * @param graph Graph
     * @param classifier Weka classifier
     * @param trainSet Train set
     * @param testSet Test set
     * @param relation relation between pair of users and instances.
     * @param attributes List of attributes and possible values for nominal attributes.
     */
    public MachineLearningWekaRecommender(FastGraph<U> graph, weka.classifiers.Classifier classifier, Instances trainSet, Instances testSet, Map<Pair<U>,Integer> relation, FastVector attributes)
    {
        super(graph);
        
        this.classifier = classifier;
        this.testSet = testSet;
        this.relation = relation;
        this.attributes = attributes;
        try
        {
            classifier.buildClassifier(trainSet);
            System.out.println("AA");
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
        this.getGraph().getAllNodes().forEach(v -> 
        {
            Pair<U> pair = new Pair<>(u,v);
            if(this.relation.containsKey(pair))
            {
                Instance ins = testSet.instance(this.relation.get(pair));
                double[] classScores;
                try 
                {
                    classScores = classifier.distributionForInstance(ins);
                    int index = ((Attribute) attributes.elementAt(testSet.classIndex())).indexOfValue(POSITIVECLASS + "");
                    scores.put(this.item2iidx(v), classScores[index]);
                } 
                catch (Exception ex) 
                {
                    scores.put(this.item2iidx(v), Double.NEGATIVE_INFINITY);
                    Exceptions.printStackTrace(ex);
                } 
            }
            else
            {
                scores.put(this.item2iidx(v), Double.NEGATIVE_INFINITY);
            }
        });
                
        return scores;
    }

    
   
}
