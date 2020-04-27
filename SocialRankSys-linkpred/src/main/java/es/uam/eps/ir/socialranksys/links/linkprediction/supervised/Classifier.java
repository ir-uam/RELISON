/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.linkprediction.supervised;

import es.uam.eps.ir.socialranksys.links.data.letor.Instance;
import es.uam.eps.ir.socialranksys.links.data.letor.InstanceSet;

import java.util.Map;

/**
 * Class that represents a binary classifier.
 * @author Javier Sanz-Cruzado Puig
 */
public interface Classifier<U> 
{
    /**
     * Trains the classifier.
     * @param trainSet The training set.
     */
    void train(InstanceSet<U> trainSet);
    
    /**
     * Computes the scores for an individual pattern (once the training has been done).
     * @param pattern The individual pattern. 
     * @return A score for each class.
     */
    Map<Integer,Double> computeScores(Instance<U> pattern);
    
    /**
     * Gets the score for an individual pattern in a certain category.
     * @param category The class.
     * @param pattern The pattern.
     * @return the score.
     */
    double computeScore(Instance<U> pattern, int category);
    /**
     * Obtains the most probable class for a certain instance.
     * @param pattern The individual instance.
     * @return The most probable class.
     */
    int classify(Instance<U> pattern);
    
}
