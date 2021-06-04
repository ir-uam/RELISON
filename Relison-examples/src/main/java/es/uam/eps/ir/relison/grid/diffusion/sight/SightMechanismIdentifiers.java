/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.diffusion.sight;

/**
 * List of identifiers of the different sight mechanisms available in the framework.
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class SightMechanismIdentifiers 
{
    // Non stop simulators (until there is no more propagation)
    public final static String ALLRECOMMENDED = "All recommended";
    public final static String ALLSIGHT = "All sight";
    public final static String ALLTRAIN = "All train";
    public final static String COUNT = "Count";
    public final static String ALLNOTDISCARDED = "All not discarded";
    public final static String ALLNOTPROPAGATED = "All not propagated";
    public final static String ALLNOTDISCARDEDNOTPROPAGATED = "All not discarded nor propagated";
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
        System.out.println("\t" + ALLNOTPROPAGATED);
        System.out.println("\t" + ALLNOTDISCARDEDNOTPROPAGATED);
        System.out.println("\t" + RECOMMENDED);
        
    }
}
