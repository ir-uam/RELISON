/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.filter;

/**
 * Identifiers for the different contact preconfigured propagation protocols available in 
 * the library
 * @author Javier Sanz-Cruzado Puig

 */
public class FilterIdentifiers 
{
    // Non stop simulators (until there is no more propagation)
    public final static String BASIC = "Basic";
    public final static String TAGSEL = "Tag Selection";
    public final static String TAG = "Tag";
    public final static String TAGCOMM = "Community Tag Selection";
    public final static String NUMTWEETS = "NumTweets";
    public final static String EMPTYTAG = "EmptyTag";
    public final static String CREATOR = "Creator";
    public final static String MINIMUMPIECES = "Minimum Pieces Feature";
    public final static String RELEVANTEDGES = "Relevant Edges";
    public final static String ONLYREPR = "Only Repropagated";
   
    /**
     * Prints the list of available algorithms
     */
    public static void printFilterList()
    {
        System.out.println("Filters:");
        System.out.println("\t" + BASIC);
        System.out.println("\t" + TAGSEL);
        System.out.println("\t" + TAG);
        System.out.println("\t" + TAGCOMM);
        System.out.println("\t" + NUMTWEETS);
        System.out.println("\t" + EMPTYTAG);
        System.out.println("\t" + CREATOR);
        System.out.println("\t" + MINIMUMPIECES);
        System.out.println("\t" + RELEVANTEDGES);
        System.out.println(ONLYREPR);
        
    }
}
