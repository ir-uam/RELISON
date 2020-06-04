/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.stop;

/**
 * Identifiers for the different stop conditions for information propagation protocols available in 
 * the library
 * @author Javier Sanz-Cruzado Puig
 */
public class StopConditionIdentifiers 
{
    // Non stop simulators (until there is no more propagation)
    public final static String NOMORENEW = "No More New";
    public final static String NOMOREPROP = "No More Propagated";
    public final static String NUMITER = "Num. Iter";
    public final static String TOTALPROP = "Total Propagated";
    public final static String NOMORETIME = "No More Timestamps";
    public final static String MAXTIME = "Max. Timestamp";
    public final static String NOMORETIMENORINFO = "No More Timestamps Nor Info";
   
    
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
