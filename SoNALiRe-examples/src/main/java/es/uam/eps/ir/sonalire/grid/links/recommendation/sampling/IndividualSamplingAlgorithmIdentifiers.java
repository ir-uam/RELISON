/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.links.recommendation.sampling;

/**
 * Identifiers for the different sampling approaches in the library.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class IndividualSamplingAlgorithmIdentifiers 
{
    // Random selection
    public final static String ALL = "All";
    public final static String DISTANCETWO = "Distance two";
    public final static String DISTANCETWOLP = "Distance two link prediction";
    public final static String LINKPRED = "Link prediction";
    public final static String RECOMMENDER = "Recommender";
    
    /**
     * Prints the list of available algorithms
     */
    public static void printPartitionAlgorithmList()
    {
        System.out.println(ALL);
        System.out.println(DISTANCETWO);
        System.out.println(DISTANCETWOLP);
        System.out.println(LINKPRED);
        System.out.println(RECOMMENDER);
    }
}
