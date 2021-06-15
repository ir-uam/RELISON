/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.diffusion.filter;

/**
 * Identifiers for the different data filters for information diffusion which are available in
 * the framework.
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class FilterIdentifiers 
{
    // Non stop simulators (until there is no more propagation)
    public final static String BASIC = "Basic";
    public final static String INFOFEATSEL = "Information feature selection";
    public final static String INFOFEAT = "Information feature";
    public final static String NUMPIECES = "Num. information pieces";
    public final static String EMPTYTAG = "Empty feature";
    public final static String CREATOR = "Creator";
    public final static String MINIMUMPIECES = "Minimum information feature frequency";
    public final static String RELEVANTEDGES = "Relevant Edges";
    public final static String ONLYREPR = "Only Repropagated";
   
    /**
     * Prints the list of available algorithms
     */
    public static void printFilterList()
    {
        System.out.println("Filters:");
        System.out.println("\t" + BASIC);
        System.out.println("\t" + INFOFEATSEL);
        System.out.println("\t" + INFOFEAT);
        System.out.println("\t" + NUMPIECES);
        System.out.println("\t" + EMPTYTAG);
        System.out.println("\t" + CREATOR);
        System.out.println("\t" + MINIMUMPIECES);
        System.out.println("\t" + RELEVANTEDGES);
        System.out.println(ONLYREPR);
        
    }
}
