/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.sight;

/**
 * Identifiers for the different sight mechanisms for information propagation protocols available in 
 * the library
 * @author Javier Sanz-Cruzado Puig
 */
public class SightMechanismIdentifiers 
{
    // Non stop simulators (until there is no more propagation)
    public final static String ALLRECOMMENDED = "All Recommended";
    public final static String ALLSIGHT = "All Sight";
    public final static String ALLTRAIN = "All Train";
    public final static String COUNT = "Count";
    public final static String ALLNOTDISCARDED = "All Not Discarded";
    public final static String RECOMMENDED = "Recommended";
   
    
    /**
     * Prints the list of available algorithms
     */
    public static void printSightMechanismList()
    {
        System.out.println("Sight Mechanisms:");
        System.out.println("\t" + ALLRECOMMENDED);
        System.out.println("\t" + ALLSIGHT);
        System.out.println("\t" + ALLTRAIN);
        System.out.println("\t" + COUNT);
        System.out.println("\t" + ALLNOTDISCARDED);
        System.out.println("\t" + RECOMMENDED);
        
    }
}
