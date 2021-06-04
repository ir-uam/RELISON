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
    public final static String NEWEST = "Newest";
    public final static String OLDER = "Older";
    
    /**
     * Prints the list of available update mechanisms
     */
    public static void printUpdateMechanismList()
    {
        System.out.println("Update Mechanisms:");
        
        System.out.println("\t" + NEWEST);
        System.out.println("\t" + OLDER);
        
        System.out.println();
    }
}
