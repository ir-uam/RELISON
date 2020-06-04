/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.propagation;

/**
 * Identifiers for the different propagation mechanisms for information diffusion protocols available in 
 * the library
 * @author Javier Sanz-Cruzado Puig
 * @see es.uam.eps.socialranksys.diffusion.propagation
 */
public class PropagationMechanismIdentifiers 
{
    // Non stop simulators (until there is no more propagation)
    public final static String ALLFOLLOWERS = "All Followers";
    public final static String PUSHPULL = "Push Pull";
    public final static String PUSH = "Push";
    public final static String PULL = "Pull";
    public final static String PUSHPULLPUREREC = "Push Pull Pure Rec";
    public final static String PUSHPULLREC = "Push Pull Rec";
    
    /**
     * Prints the list of available algorithms
     */
    public static void printSelectionMechanismList()
    {
        System.out.println("Selection Mechanism:");
        System.out.println("\t" + ALLFOLLOWERS);
        System.out.println("\t" + PUSHPULL);
        System.out.println("\t" + PUSH);
        System.out.println("\t" + PULL);
        System.out.println("\t" + PUSHPULLREC);
        System.out.println("\t" + PUSHPULLPUREREC);
    }
}
