/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.sampling;

/**
 * Identifiers for the different contact recommendation algorithms available in 
 * the library
 * @author Javier Sanz-Cruzado Puig
 */
public class IndividualSamplingAlgorithmIdentifiers 
{
    // Random selection
    public final static String DISTANCETWO = "Distance Two";
    public final static String DISTANCETWOLP = "Distance Two Link Prediction";
    public final static String RECOMMENDER = "Recommender";
    
    /**
     * Prints the list of available algorithms
     */
    public static void printPartitionAlgorithmList()
    {
        System.out.println(DISTANCETWO);
        System.out.println(DISTANCETWOLP);
        System.out.println(RECOMMENDER);
    }
}
