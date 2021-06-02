/*
 *  Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.linkprediction.supervised.classifiers;


import es.uam.eps.ir.ranksys.core.util.Stats;
import es.uam.eps.ir.sonalire.links.data.letor.*;
import es.uam.eps.ir.sonalire.links.linkprediction.supervised.Classifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Classifier which applies the Naive Bayes method. In case the attributes are continuous,
 * Gaussian Naive Bayes is applied to compute the scores.
 * 
 * Note: For computing the means and variances, an incremental algorithm is used. This algorithm
 * is documented in:
 * 
 * 
 * 
 * @author Javier Sanz-Cruzado Puig
 */
public class NaiveBayesClassifier<U> implements Classifier<U>
{    
    /**
     * Indicates if the training of the classifier has been done.
     */
    private boolean trained;
    
    /**
     * Times each class appears in the training set.
     */
    private List<Double> priori;
    
    /**
     * A list of matrices which contains the information for each attribute in the training set.
     * For each matrix, the columns represent the different classes.
     * 
     * In the case of nominal attributes, each row represents a different value of the attributes,
     * and each cell contains the number of different examples which share the same value of the attribute
     * and class.
     * 
     * In the case of continuous attributes, first row represents the mean of the sample, and second row
     * represents the variance of the sample.
     */
    private List<Double[][]> frequencies;
    /**
     * List of classes.
     */
    private List<Integer> classes;
    /**
     * List of attributes.
     */
    private List<String> attributes;
    
    /**
     * Constant index for the mean.
     */
    private final static int MEAN = 0;
    /**
     * Constant index for the variance.
     */
    private final static int SIGMA = 1;
    /**
     * Number of parameters in .
     */
    private final static int NUMCONT = 2;
    /**
     * The number of patterns.
     */
    private int numInstances;
    /**
     * The stats for each attribute.
     */
    private List<Stats> stats;
    /**
     * The types for each attribute.
     */
    private List<FeatureType> types;
    /**
     * Indicates if attributes have to be normalized or not.
     */
    private final boolean normalize;

    /**
     * Constructor.
     */
    public NaiveBayesClassifier() 
    {
       this.trained = false;
       this.normalize = false;
    }
    
    /**
     * Constructor
     * @param normalize indicates if attributes have to be normalized
     */
    public NaiveBayesClassifier(boolean normalize)
    {
        this.normalize = normalize;
    }

    @Override
    public void train(InstanceSet<U> trainSet)
    {
        FeatureInformation featInfo = trainSet.getFeatInfo();
        
        if(normalize)
            this.stats = featInfo.getStats();
        this.types = featInfo.getFeatureTypes();
        
        
        this.trained = false;
        // Store the classes.
        this.classes = new ArrayList<>(trainSet.getClasses());
        int nClasses = trainSet.getClasses().size();
                
        this.priori = new ArrayList<>();
        for(int i = 0; i < nClasses; ++i)
            this.priori.add(0.0);
        
        this.attributes = featInfo.getFeatureDescriptions();
        int nAttrib = featInfo.numFeats();
        this.frequencies = new ArrayList<>();
        
        for(int i = 0; i < nAttrib; ++i)
        {
            if(featInfo.getFeatureType(i).equals(FeatureType.NOMINAL))
            {
                NominalStats nomStats = (NominalStats) featInfo.getStats(i);
                int numValues = nomStats.getNumValues();
                
                this.frequencies.add(new Double[numValues + 1][nClasses]);
                for(int j = 0; j < numValues + 1; ++j)
                {
                    for(int k = 0; k < nClasses; ++k)
                    {
                        this.frequencies.get(i)[j][k] = 0.0;
                    }
                }
            }
            else //if(attributes.get(i).getSecond().equals(AttrType.CONTINUOUS))
            {
                this.frequencies.add(new Double[NUMCONT][nClasses]);
                for(int j = 0; j < nClasses; ++j)
                {
                     this.frequencies.get(i)[MEAN][j] = 0.0;
                     this.frequencies.get(i)[SIGMA][j] = 0.0;
                }
            }
            
        }
        
        this.numInstances = trainSet.getNumInstances();
        
        trainSet.getAllInstances().forEach(pattern -> 
        {
            int category = pattern.getCategory();
            // Update the prioris
            this.priori.set(category, this.priori.get(category) + 1.0);
            
            for(int i = 0; i < nAttrib; ++i)
            {
                if(featInfo.getFeatureType(i).equals(FeatureType.NOMINAL))
                {
                    int atribValue = pattern.getValues().get(i).intValue();
                    this.frequencies.get(i)[atribValue][category]++;
                    this.frequencies.get(i)[this.frequencies.get(i).length - 1][category]++; // Counters
                }
                else // if(this.attributes.get(i).getSecond().equals(AttrType.CONTINUOUS)
                {
                    double oldMean = this.frequencies.get(i)[MEAN][category];
                    double oldVariance = this.frequencies.get(i)[SIGMA][category];
                    double value =  pattern.getValue(i);

                    if(this.normalize)
                    {
                        value = (value - stats.get(i).getMean())/(stats.get(i).getStandardDeviation());
                    }

                    this.frequencies.get(i)[MEAN][category] += (value - oldMean)/(this.priori.get(category));
                    this.frequencies.get(i)[SIGMA][category] += (value - oldMean)*(value - this.frequencies.get(i)[MEAN][category]);
                }
            }
        });
        
        // Update the mean and variance values for continuous attributes.
        for(int i = 0; i < nAttrib; ++i)
        {
            /*if(this.attributes.get(i).v2().equals(AttrType.CONTINUOUS))
            {*/
            for(int j = 0; j < nClasses; ++j)
            {
                this.frequencies.get(i)[SIGMA][j] /= (this.priori.get(j) - 1.0);
            }
            //}
        }
        
        this.trained = true;
    }

    @Override
    public Map<Integer, Double> computeScores(Instance<U> instance)
    {
        if(!this.trained)
            return null;
        
        int nClasses = this.classes.size();
        
        Map<Integer, Double> scores = new HashMap<>();
        for (Integer aClass : this.classes)
        {
            scores.put(aClass, this.computeScore(instance, aClass));
        }
        
        return scores;
    }

    @Override
    public double computeScore(Instance<U> instance, int category)
    {
        if(!this.trained || !this.classes.contains(category))
            return Double.NaN;      
        
        int cat = this.classes.indexOf(category);
        int nAttrib = this.attributes.size();
        double score = 1.0;
        for(int i = 0; i < nAttrib; ++i)
        {
            if(this.types.get(i).equals(FeatureType.NOMINAL))
            {
                double val = instance.getValue(i);
                int idx = ((NominalStats) this.stats.get(i)).indexOfValue(val);
                
                int l = this.frequencies.get(i).length;
                double aux = (this.frequencies.get(i)[idx][cat] + 1.0)/(this.frequencies.get(i)[l-1][cat] + l + 0.0);

                // Apply the Laplace smoothing
                score *= aux;  
            }
            else // if(this.attributes.get(i).getSecond().equals(AttrType.CONTINUOUS))
            {
                double mean = this.frequencies.get(i)[MEAN][cat];
                double var = this.frequencies.get(i)[SIGMA][cat];

                if(var == 0.0)
                    continue;

                double value = instance.getValue(i);
                if(this.normalize)
                    value = (instance.getValue(i) - stats.get(i).getMean())/stats.get(i).getStandardDeviation();
                double aux = (value - mean)*(value - mean)/(2*var);
                double aux1 = Math.exp(-aux);
                double aux2 = (value - mean)*(value - mean)/(2*var);
                aux =  Math.exp(-aux)/ Math.sqrt(2*Math.PI*var);

                score *= aux;
            }
        }
        
        return score*this.priori.get(cat)/(this.numInstances+0.0);
    }

    @Override
    public int classify(Instance<U> instance) {
        if(!this.trained)
            return -1;
        
        Map<Integer, Double> scores = this.computeScores(instance);
        
        int currentClass = -1;
        double max = Double.NEGATIVE_INFINITY;
        
        for(int category : scores.keySet())
        {
            if(scores.get(category) > max)
            {
                max = scores.get(category);
                currentClass = category;
            }
        }
        
        return currentClass;
    }

    
}
