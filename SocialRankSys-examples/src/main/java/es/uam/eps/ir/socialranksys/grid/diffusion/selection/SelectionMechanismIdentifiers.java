/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.selection;

/**
 * Identifiers for the different selection mechanisms for information propagation
 * available in the library
 * @author Javier Sanz-Cruzado Puig
 */
public class SelectionMechanismIdentifiers 
{
    // Non stop simulators (until there is no more propagation)
    public final static String COUNT = "Count";
    public final static String ICM = "Independent Cascade Model";
    public final static String PUSHPULL = "Push Pull";
    public final static String REC = "Recommender";
    public final static String PUREREC = "Pure Recommender";
    public final static String PURERECBATCH = "Pure Recommender Batch";
    public final static String THRESHOLD = "Threshold";
    public final static String COUNTTHRESHOLD = "Count Threshold";
    public final static String ALLREALPROP = "All Real Propagated";
    public final static String COUNTREALPROP = "Count Real Propagated";
    public final static String ONLYOWN = "Only Own";
    public final static String PURETIMESTAMP = "Pure Timestamp-Based";
    public final static String LOOSETIMESTAMP = "Loose Timestamp-Based";
    public final static String TIMESTAMPORDERED = "Timestamp-Ordered";
   
    
    /**
     * Prints the list of available selection mechanisms
     */
    public static void printSelectionMechanismList()
    {
        System.out.println("Selection Mechanism:");
        System.out.println("\t" + COUNT);
        System.out.println("\t" + ICM);
        System.out.println("\t" + PUSHPULL);
        System.out.println("\t" + REC);
        System.out.println("\t" + PUREREC);
        System.out.println("\t" + PURERECBATCH);
        System.out.println("\t" + THRESHOLD);
        System.out.println("\t" + COUNTTHRESHOLD);
        System.out.println("\t" + ALLREALPROP);
        System.out.println("\t" + COUNTREALPROP);
        System.out.println("\t" + ONLYOWN);
        System.out.println("\t" + PURETIMESTAMP);
        System.out.println("\t" + LOOSETIMESTAMP);
        System.out.println("\t" + TIMESTAMPORDERED);       
    }
}
