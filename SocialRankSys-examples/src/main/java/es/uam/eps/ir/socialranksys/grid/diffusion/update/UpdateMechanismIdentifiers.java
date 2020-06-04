/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.update;

/**
 * Identifiers for the different update mechanisms for information propagation protocols available in 
 * the library
 * @author Javier Sanz-Cruzado Puig
 */
public class UpdateMechanismIdentifiers 
{
    
    public final static String ICM = "Independent Cascade Model";
    public final static String OLDER = "Older";
    
    /**
     * Prints the list of available update mechanisms
     */
    public static void printUpdateMechanismList()
    {
        System.out.println("Update Mechanisms:");
        
        System.out.println("\t" + ICM);
        System.out.println("\t" + OLDER);
        
        System.out.println();
    }
}
