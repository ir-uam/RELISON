/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.diffusion.metrics.distributions;

/**
 * The list of identifiers of the distributions.
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class DistributionIdentifiers 
{
    /**
     * Identifiers for the information features.
     */
    public final static String INFOFEATS = "Information features";
    /**
     * Identifiers for the user features.
     */
    public final static String USERFEATS = "User features";
    /**
     * Identifiers for the information.
     */
    public final static String INFORMATION = "Information";
    /**
     * Identifiers for the mixed features.
     */
    public final static String MIXEDFEATS = "Mixed features";
    /**
     * Identifiers for the users.
     */
    public final static String USERS = "Users";
    
    /**
     * Prints the list of available distributions
     */
    public static void printDistributionList()
    {
        System.out.println("Metrics:");
        System.out.println("\t" + INFOFEATS + "\n");
        System.out.println("\t" + USERFEATS + "\n");
        System.out.println("\t" + INFORMATION + "\n");
        System.out.println("\t" + MIXEDFEATS + "\n");
        System.out.println("\t" + USERS + "\n");
        System.out.println();
    }
}
