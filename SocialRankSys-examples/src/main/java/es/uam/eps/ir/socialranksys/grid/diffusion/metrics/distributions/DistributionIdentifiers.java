/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.metrics.distributions;

/**
 * Identifiers for the different distributions available in 
 * the library
 * @author Javier Sanz-Cruzado Puig
 * @see es.uam.eps.socialranksys.diffusion.metrics.distributions
 */
public class DistributionIdentifiers 
{
    public final static String INFOPARAM = "Information Param.";
    public final static String USERPARAM = "User Param.";
    public final static String INFORMATION = "Information";
    public final static String MIXEDPARAM = "Mixed Param.";
    
    /**
     * Prints the list of available distributions
     */
    public static void printDistributionList()
    {
        System.out.println("Metrics:");
        System.out.println("\t" + INFOPARAM + "\n");
        System.out.println("\t" + USERPARAM + "\n");
        System.out.println("\t" + INFORMATION + "\n");
        System.out.println("\t" + MIXEDPARAM + "\n");
        System.out.println();
    }
}
