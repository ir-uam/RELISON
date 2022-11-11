/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.diffusion.update;

/**
 * Identifiers for the update mechanisms for the information diffusion available in the framework.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 */
public class UpdateMechanismIdentifiers 
{
    /**
     * Identifier for the update mechanism that keeps the newest piece of information.
     */
    public final static String NEWEST = "Newest";
    /**
     * Identifier for the update mechanism that merges the newest and the oldest piece of information.
     */
    public final static String MERGER = "Merger";
    /**
     * Identifier for the update mechanism that merges the oldest piece of information.
     */
    public final static String OLDEST = "Oldest";
    
    /**
     * Prints the list of available update mechanisms
     */
    public static void printUpdateMechanismList()
    {
        System.out.println("Update Mechanisms:");

        System.out.println("\t" + MERGER);
        System.out.println("\t" + NEWEST);
        System.out.println("\t" + OLDEST);
        
        System.out.println();
    }
}
