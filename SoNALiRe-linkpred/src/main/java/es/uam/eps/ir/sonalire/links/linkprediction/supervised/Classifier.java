/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.linkprediction.supervised;

import es.uam.eps.ir.sonalire.links.data.letor.Instance;
import es.uam.eps.ir.sonalire.links.data.letor.InstanceSet;

import java.util.Map;

/**
 * Methods for defining a supervised machine learning classifier.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface Classifier<U> 
{
    /**
     * Trains the classifier.
     * @param trainSet the training set.
     */
    void train(InstanceSet<U> trainSet);
    
    /**
     * Computes the scores for an individual instance (once the training has been done).
     * @param instance the individual instance.
     * @return A score for each class.
     */
    Map<Integer,Double> computeScores(Instance<U> instance);
    
    /**
     * Gets the score for an individual pattern in a certain category.
     * @param category the class.
     * @param instance the pattern.
     * @return the score.
     */
    double computeScore(Instance<U> instance, int category);

    /**
     * Obtains the most likely class for a certain instance.
     * @param instance the individual instance.
     * @return The most likely class.
     */
    int classify(Instance<U> instance);
    
}
