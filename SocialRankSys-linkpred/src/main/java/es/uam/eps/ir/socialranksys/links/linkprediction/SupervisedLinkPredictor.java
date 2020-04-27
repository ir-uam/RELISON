/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.linkprediction;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.links.data.ml.MLConstants;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;
import org.openide.util.Exceptions;
import org.ranksys.core.util.tuples.Tuple2od;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Supervised method for link prediction (WEKA)
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public class SupervisedLinkPredictor<U> extends AbstractLinkPredictor<U>
{
    /**
     * Weka classifier. It provides the scores for each element.
     */
    private final Classifier classifier;
    /**
     * Training instances. They are used for building the model
     */
    private final Instances trainInstances;
    /**
     * Test instances. Represent the edges we want to predict.
     */
    private final Instances testInstances;
    /**
     * Relation between pairs of users and the test patterns
     */
    private final Map<Pair<U>,Integer> relation;
    /**
     * Attributes of the patterns.
     */
    private final FastVector attributes;
    
    /**
     * Constructor
     * @param graph the graph.
     * @param comparator comparator for ordering the different pairs.
     * @param classifier a classifier that performs the prediction.
     * @param trainInstances instances for building the model.
     * @param testInstances instances for applying the model.
     * @param relation relation between pairs of users and test instances.
     * @param attributes the list of attributes.
     */
    public SupervisedLinkPredictor(Graph<U> graph, Comparator<Tuple2od<Pair<U>>> comparator, Classifier classifier, Instances trainInstances, Instances testInstances, Map<Pair<U>,Integer> relation, FastVector attributes)
    {
        super(graph, comparator);
        this.classifier = classifier;
        this.trainInstances = trainInstances;
        this.testInstances = testInstances;
        this.relation = relation;
        this.attributes = attributes;
        try {
            this.classifier.buildClassifier(this.trainInstances);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    
    @Override
    public List<Tuple2od<Pair<U>>> getPrediction(int maxLength, Predicate<Pair<U>> filter)
    {
        SortedSet<Tuple2od<Pair<U>>> auxSet = new TreeSet<>(this.getComparator());
        
        this.getGraph().getAllNodes().forEach(u -> 
        {
            Set<U> vFilter = this.getGraph().getAllNodes().filter(v -> filter.test(new Pair<>(u,v))).collect(Collectors.toCollection(HashSet::new));
            if(!vFilter.isEmpty())
            {
                vFilter.forEach(v -> 
                {
                    Pair<U> pair = new Pair<>(u,v);
                    if(this.relation.containsKey(pair))
                    {
                        Instance ins = this.testInstances.instance(this.relation.get(pair));
                        double[] classScores;
                        try 
                        {
                            classScores = classifier.distributionForInstance(ins);
                            int index = ((Attribute) attributes.elementAt(testInstances.classIndex())).indexOfValue(MLConstants.POSITIVECLASS + "");
                            auxSet.add(new Tuple2od<>(pair, classScores[index]));
                        } 
                        catch (Exception ex) 
                        {
                            auxSet.add(new Tuple2od<>(pair, Double.NEGATIVE_INFINITY));
                            Exceptions.printStackTrace(ex);
                        }
                        
                    }
                    else
                    {
                        auxSet.add(new Tuple2od<>(pair, Double.NEGATIVE_INFINITY));
                    }
                });
            }
        });

        return auxSet.stream().limit(maxLength).collect(Collectors.toList());
    }
    
}
