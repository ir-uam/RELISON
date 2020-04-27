/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.linkprediction.supervised.classifiers;

import es.uam.eps.ir.ranksys.core.util.Stats;
import es.uam.eps.ir.socialranksys.links.data.letor.FeatureInformation;
import es.uam.eps.ir.socialranksys.links.data.letor.Instance;
import es.uam.eps.ir.socialranksys.links.data.letor.InstanceSet;
import es.uam.eps.ir.socialranksys.links.linkprediction.supervised.Classifier;
import es.uam.eps.ir.socialranksys.utils.datatypes.Vector;
import es.uam.eps.ir.socialranksys.utils.math.MathFunctions;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Classifier that applies the logistic regression classifier
 * 
 * Bishop,C.M.Instance Recognition and Machine Learning, Springer, 2006, pp. 205-207
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public class LogisticRegressionClassifier<U> implements Classifier<U>
{
    /**
     * Number of attributes of the problem, including the class.
     */
    private int dimension;
    
    /**
     * Vector that determines the hyperplane which separates both classes.
     */
    private Vector omega;
    
    /**
     * Learning coefficient of the classifier. Controls the convergence speed of the 
     * classifier. Greater values may lead to bad convergence, but very small values
     * make the convergence very slow.
     */
    private final double eta;
    
    /**
     * Maximum number of iterations which the algorithm will run during training.
     */
    private final int maxEpochs;
    
    /**
     * Minimum possible initial value for each coordinate of the omega vector
     */
    private final static double RANGEMIN = -1.0;
    /**
     * Maximum possible initial value for each coordinate of the omega vector
     */
    private final static double RANGEMAX = 1.0;
    
    /**
     * When the difference between the previous iteration of the algorithm and the
     * current is smaller than this value, the algorithm stops.
     */
    private final static double THRESHOLD = 1e-8;
    
    /**
     * List of classes.
     */
    private List<Integer> classes;
    
    /**
     * True if the patterns have to be normalized.
     */
    private final boolean normalize;

    /**
     * The list of stats for each attribute.
     */
    private List<Stats> stats;
    
    /**
     * Indicates if the classifier has been trained.
     */
    private boolean trained;
    
    public LogisticRegressionClassifier(double eta, int maxEpochs)
    {
        this(eta, maxEpochs, false);
    }
    
    /**
     * Constructor.
     * @param eta Learning coefficient of the classifier.
     * @param maxEpochs Maximum number of iterations.
     * @param normalize indicates if the data has to be normalized.
     */
    public LogisticRegressionClassifier(double eta, int maxEpochs, boolean normalize)
    {
        this.eta = eta;
        if(maxEpochs > 0)
            this.maxEpochs = maxEpochs;
        else
            this.maxEpochs = 1;
        this.trained = false;
        this.normalize = normalize;
    }
    
    @Override
    public void train(InstanceSet<U> trainSet) {
        trained = false;
        FeatureInformation featInfo = trainSet.getFeatInfo();

        dimension = featInfo.numFeats() + 1;
        omega = new Vector(dimension);
        Vector x = new Vector(dimension);
        Random r = new Random();
        
        classes = new ArrayList<>(trainSet.getClasses());
        double change = Double.MAX_VALUE;
        
        if(classes.size() != 2)
            return;
        
        stats = featInfo.getStats();
        
        
        // Initializing the omega values with real values between RANGEMIN and RANGEMAX
        for(int i =0 ; i < dimension; ++i)
        {
            omega.set(i,RANGEMIN + (RANGEMAX - RANGEMIN) * r.nextDouble());
        }
        
        List<Instance<U>> patterns = trainSet.getAllInstances().collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(patterns);
        
        // Iterate while the number of epochs is not reached, and the variation is smaller than the threshold
        for(int i = 0; i < maxEpochs && (change >= THRESHOLD); ++i)
        {
           change = 0.0;
           
           // Iterate over each pattern
           for(Instance<U> pattern : patterns)
           {
               // Get the vector for the pattern
               for(int j = 0; j < dimension - 1; ++j)
               {
                    if(this.normalize)
                    {
                        x.set(j, (pattern.getValue(j) - stats.get(j).getMean())/ stats.get(j).getStandardDeviation());
                    }
                    else
                    {
                        x.set(j, pattern.getValue(j));
                    }
               }
               x.set(dimension - 1, 1.0);
               
               // Compute the updating factors:
               double y = MathFunctions.sigmoid.applyAsDouble(omega.scalarProd(x));
               double t = pattern.getCategory() == 0 ? 1.0 : 0.0;
               
               for(int j = 0; j < dimension; ++j)
               {
                   // Update the vector
                   omega.set(j, omega.get(j) - eta*(y-t)*y*(1-y)*x.get(j));
               }
               change += (y-t)*(y-t);
           }
           
           change /= (trainSet.getNumInstances() + 0.0);
        }
        
        
        trained = true;
    }

    @Override
    public Map<Integer, Double> computeScores(Instance<U> pattern) 
    {
        if(!this.trained)
            return null;
        
        Map<Integer, Double> scores = new HashMap<>();
        
        double score = this.computeScore(pattern, this.classes.get(0));
        
        scores.put(this.classes.get(0), score);
        scores.put(this.classes.get(1), 1.0 - score);
        return scores;
    }

    @Override
    public double computeScore(Instance<U> pattern, int category) 
    {
        if(!this.classes.contains(category))
            return Double.NaN;
        
        if(category == this.classes.get(0))
        {
            Vector x = new Vector(this.dimension);
            for(int j = 0; j < dimension - 1; ++j)
            {
                if(this.normalize)
                {
                    x.set(j, (pattern.getValue(j) - stats.get(j).getMean())/ stats.get(j).getStandardDeviation());
                }
                else
                {
                    x.set(j, pattern.getValue(j));
                }
            }
            x.set(dimension-1, 1.0);
            
            return MathFunctions.sigmoid.applyAsDouble(omega.scalarProd(x));
        }
        return 1.0 - this.computeScore(pattern, this.classes.get(0));
    }

    @Override
    public int classify(Instance<U> pattern) {
        if(computeScore(pattern, this.classes.get(0)) > 0.5)
            return classes.get(0);
        else
            return classes.get(1);
    }
    
}
