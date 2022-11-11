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
    /**
     * Identifier for seeing all the contents from recommended links.
     */
    public final static String ALLRECOMMENDED = "All recommended";
    /**
     * Identifier for seeing all the contents.
     */
    public final static String ALLSIGHT = "All sight";
    /**
     * Identifier for seeing all the contents not from recommended links.
     */
    public final static String ALLTRAIN = "All train";
    /**
     * Identifier for seeing a limited amount of information pieces.
     */
    public final static String COUNT = "Count";
    /**
     * Identifier for seeing all the not previously discarded contents.
     */
    public final static String ALLNOTDISCARDED = "All not discarded";
    /**
     * Identifier for seeing all the not previously propagated contents.
     */
    public final static String ALLNOTPROPAGATED = "All not propagated";
    /**
     * Identifier for seeing all the not previously discarded nor propagated contents.
     */
    public final static String ALLNOTDISCARDEDNOTPROPAGATED = "All not discarded nor propagated";
    /**
     * Identifier for seeing contents from recommended links or not recommended links from certain probability.
     */
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
