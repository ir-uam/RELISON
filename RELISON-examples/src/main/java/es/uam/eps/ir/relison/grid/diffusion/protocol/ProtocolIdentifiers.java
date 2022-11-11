/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.diffusion.protocol;

/**
 * Identifiers for the different preconfigured propagation protocols which are available
 * in the library.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class ProtocolIdentifiers 
{
    // Non stop simulators (until there is no more propagation)
    /**
     * Identifier for the independent cascade model protocol.
     */
    public final static String INDEPCASCADE = "Independent cascade model";
    /**
     * Identifier for the simple diffusion protocol.
     */
    public final static String SIMPLE = "Simple";
    /**
     * Identifier for the Push protocol.
     */
    public final static String PUSH = "Push";
    /**
     * Identifier for the Pull protocol.
     */
    public final static String PULL = "Pull";
    /**
     * Identifier for the rumor spreading protocol.
     */
    public final static String RUMORSPREADING = "Rumor spreading";
    /**
     * Identifier for the bidirectional rumor spreading protocol.
     */
    public final static String BIDIRRUMORSPREADING = "Bidirectional rumor spreading";
    /**
     * Identifier for the proportion threshold protocol.
     */
    public final static String THRESHOLD = "Proportion threshold";
    /**
     * Identifier for the count threshold protocol.
     */
    public final static String COUNTTHRESHOLD = "Count threshold";
    /**
     * Identifier for the temporal protocol.
     */
    public final static String TEMPORAL = "Temporal";
    
    /**
     * Prints the list of available algorithms
     */
    public static void printProtocolList()
    {
        System.out.println("Non Stop simulators:");
        System.out.println("\t" + INDEPCASCADE);
        System.out.println("\t" + SIMPLE);
        System.out.println("\t" + PUSH);
        System.out.println("\t" + PULL);
        System.out.println("\t" + RUMORSPREADING);
        System.out.println("\t" + BIDIRRUMORSPREADING);
        System.out.println("\t" + THRESHOLD);
        System.out.println("\t" + COUNTTHRESHOLD);
        System.out.println("\t" + TEMPORAL);
        System.out.println();
    }
}
