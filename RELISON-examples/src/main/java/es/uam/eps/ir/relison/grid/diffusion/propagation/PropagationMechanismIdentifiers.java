/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.diffusion.propagation;

/**
 * Identifiers for the different propagation mechanisms for information diffusion protocols available in 
 * the framework.
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class PropagationMechanismIdentifiers 
{
    // Non stop simulators (until there is no more propagation)

    /**
     * Identifier for the propagation mechanism to all neighbors.
     */
    public final static String ALLNEIGHS = "All neighbors";
    /**
     * Identifier for the propagation mechanism to all recommended neighbors.
     */
    public final static String ALLRECNEIGHS = "All recommended neighbors";
    /**
     * Identifier for the propagation mechanism related to the push-pull protocol.
     */
    public final static String PUSHPULL = "Push-pull";
    /**
     * Identifier for the propagation mechanism related to the push protocol.
     */
    public final static String PUSH = "Push";
    /**
     * Identifier for the propagation mechanism related to the pull protocol.
     */
    public final static String PULL = "Pull";
    /**
     * Identifier for the propagation mechanism related to the push-pull protocol, to use only recommended links.
     */
    public final static String PUSHPULLPUREREC = "Push-pull pure recommended";
    /**
     * Identifier for the propagation mechanism related to the push-pull protocol, prioritizing recommended links.
     */
    public final static String PUSHPULLREC = "Push-pull recommended";
    
    /**
     * Prints the list of available algorithms
     */
    public static void printSelectionMechanismList()
    {
        System.out.println("Selection Mechanism:");
        System.out.println("\t" + ALLNEIGHS);
        System.out.println("\t" + ALLRECNEIGHS);
        System.out.println("\t" + PUSHPULL);
        System.out.println("\t" + PUSH);
        System.out.println("\t" + PULL);
        System.out.println("\t" + PUSHPULLREC);
        System.out.println("\t" + PUSHPULLPUREREC);
    }
}
