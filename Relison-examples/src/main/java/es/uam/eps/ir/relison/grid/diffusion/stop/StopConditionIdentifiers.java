/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.diffusion.stop;

/**
 * Identifiers for the different stop conditions for the simulation of information propagation
 * which are available in the framework.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class StopConditionIdentifiers 
{
    // Non stop simulators (until there is no more propagation)
    public final static String NOMORENEW = "No more new";
    public final static String NOMOREPROP = "No more propagated";
    public final static String NUMITER = "Num. iter";
    public final static String TOTALPROP = "Total propagated";
    public final static String NOMORETIME = "No more timestamps";
    public final static String MAXTIME = "Max. timestamp";
    public final static String NOMORETIMENORINFO = "No more timestamps nor info";
   
    
    /**
     * Prints the list of available algorithms
     */
    public static void printStopConditionList()
    {
        System.out.println("Stop Conditions:");
        System.out.println("\t" + NOMORENEW);
        System.out.println("\t" + NOMOREPROP);
        System.out.println("\t" + NUMITER);
        System.out.println("\t" + TOTALPROP);
        System.out.println("\t" + NOMORETIME);
        System.out.println("\t" + MAXTIME);
        System.out.println("\t" + NOMORETIMENORINFO);
    }
}
