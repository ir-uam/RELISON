/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.algorithms.supervised;

import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.links.data.letor.InstanceSet;
import es.uam.eps.ir.relison.links.data.letor.io.InstanceSetReader;
import es.uam.eps.ir.relison.links.data.letor.io.LETORInstanceReader;
import es.uam.eps.ir.relison.links.data.ml.MLConstants;
import es.uam.eps.ir.relison.links.data.ml.weka.WekaInstanceReader;
import es.uam.eps.ir.relison.links.data.ml.weka.WekaInstanceSet;
import es.uam.eps.ir.relison.links.data.ml.weka.WekaInstanceTranslator;
import es.uam.eps.ir.relison.links.recommendation.UserFastRankingRecommender;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import org.ranksys.formats.parsing.Parser;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import java.io.IOException;
import java.util.Map;


/**
 * Contact recommendation algorithm that uses supervised classification techniques to generate the
 * recommendation. It uses the Weka library.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class MachineLearningWekaRecommender<U> extends UserFastRankingRecommender<U>
{
    /**
     * Weka classifier.
     */
    private final Classifier classifier;
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
     * Constructor. Reads the instances from three Weka files: a training set, a test set and a feature data file.
     * @param graph             the training network.
     * @param classifier        the classifier to apply.
     * @param trainInstances    a file containing the training instances.
     * @param testInstances     a file containing the test instances.
     * @param featureData       a file containing the feature information.
     * @param parser            the user parser.
     */
    public MachineLearningWekaRecommender(FastGraph<U> graph, Classifier classifier, String trainInstances, String testInstances, String featureData, Parser<U> parser)
    {
        super(graph);

        WekaInstanceReader<U> reader = new WekaInstanceReader<>();
        reader.readFeatures(featureData);
        reader.readTrain(trainInstances);
        reader.readTest(testInstances, parser);

        Instances trainSet = reader.getTrainSet();
        this.testSet = reader.getTestSet();
        this.attributes = reader.getFVAttributes();
        this.relation = reader.getInstanceIndexer();
        this.classifier = classifier;

        try
        {
            this.classifier.buildClassifier(trainSet);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    /**
     * Constructor. Reads the instances from two files in the LETOR format.
     * @param graph             the training network.
     * @param classifier        the classifier to apply.
     * @param trainInstances    a file containing the training instances.
     * @param testInstances     a file containing the test instances.
     * @param parser            the user parser.
     * @throws IOException      if something fails while reading the files.
     */
    public MachineLearningWekaRecommender(FastGraph<U> graph, Classifier classifier, String trainInstances, String testInstances, Parser<U> parser) throws IOException
    {
        super(graph);

        InstanceSetReader<U> reader = new LETORInstanceReader<>(parser);
        InstanceSet<U> train = reader.read(trainInstances);
        InstanceSet<U> test = reader.read(testInstances);

        WekaInstanceTranslator<U> translator = new WekaInstanceTranslator<>();
        WekaInstanceSet<U> wekaTrain = translator.toWeka(train, "train");
        WekaInstanceSet<U> wekaTest = translator.toWeka(test, "test");

        Instances trainSet = wekaTrain.getInstances();
        this.testSet = wekaTest.getInstances();
        this.attributes = wekaTest.getFeatures();
        this.relation = wekaTest.getInstanceMap();
        this.classifier = classifier;

        try
        {
            this.classifier.buildClassifier(trainSet);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }


    /**
     * Constructor
     * @param graph      the training graph.
     * @param classifier Weka classifier
     * @param trainSet   instances in the training set
     * @param testSet    instances in the test set.
     * @param relation   relation between pairs of users and instances.
     * @param attributes list of attributes and possible values for nominal attributes.
     */
    public MachineLearningWekaRecommender(FastGraph<U> graph, Classifier classifier, Instances trainSet, Instances testSet, Map<Pair<U>,Integer> relation, FastVector attributes)
    {
        super(graph);
        
        this.classifier = classifier;
        this.testSet = testSet;
        this.relation = relation;
        this.attributes = attributes;
        try
        {
            classifier.buildClassifier(trainSet);
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
                    int index = ((Attribute) attributes.elementAt(testSet.classIndex())).indexOfValue(MLConstants.POSITIVECLASS + "");
                    scores.put(this.item2iidx(v), classScores[index]);
                } 
                catch (Exception ex) 
                {
                    scores.put(this.item2iidx(v), Double.NEGATIVE_INFINITY);
                    ex.printStackTrace();
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
