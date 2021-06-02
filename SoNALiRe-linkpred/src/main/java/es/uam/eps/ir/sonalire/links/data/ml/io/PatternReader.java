/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.data.ml.io;

import es.uam.eps.ir.sonalire.links.data.ml.features.Features;
import org.ranksys.formats.parsing.Parser;

/**
 * Class for reading machine learning patterns.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <S> type of the instance set.
 * @param <I> type of an individual instance.
 */
public interface PatternReader<U,S,I> 
{
    /**
     * Configures the features of the dataset.
     * @param featureFile the file containing the description for the dataset features.
     * @return true if everything went OK, false if not.
     */
    boolean readFeatures(String featureFile);
    /**
     * Reads the training set.
     * @param trainFile file which contains the training instances.
     * @return true if everything went ok, false if not.
     */
    boolean readTrain(String trainFile);
    /**
     * Reads the test set.
     * @param testFile  file which contains the test instances
     * @param parser    parser for the user identifiers.
     * @return true if everything went OK, false if not.
     */
    boolean readTest(String testFile, Parser<U> parser);
    
    /**
     * Obtains the training set instances
     * @return the training set instances
     */
    S getTrainSet();
    
    /**
     * Obtains the test set instances
     * @return the test set instances
     */
    S getTestSet();
    
    /**
     * Obtains a test instance for a pair of nodes
     * @param u the first node.
     * @param v the second node.
     * @return null if the instance does not exist, a instance if it does.
     */
    I getTestInstance(U u, U v);
    
    /**
     * Gets the features.
     * @return the features.
     */
    Features getFeatures();
}
