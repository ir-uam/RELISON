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
    /**
     * Identifier for the basic filter (does nothing)
     */
    public final static String BASIC = "Basic";
    /**
     * Identifier for the basic filter (does nothing)
     */
    public final static String INFOFEATSEL = "Information feature selection";
    /**
     * Identifier that filters selected information features.
     */
    public final static String INFOFEAT = "Information feature";
    /**
     * Identifier for the filter that indicates a maximum number of pieces that a single user might have.
     */
    public final static String NUMPIECES = "Num. information pieces";
    /**
     * Identifier for the filter which adds a new feature for empty feature pieces.
     */
    public final static String EMPTYTAG = "Empty feature";
    /**
     * Identifier for the basic filter which only keeps information pieces with creator.
     */
    public final static String CREATOR = "Creator";

    /**
     * Identifier for the filter which removes those information features appearing less than a fixed number of times.
     */
    public final static String MINIMUMPIECES = "Minimum information feature frequency";
    /**
     * Identifier for the filter which removes recommended but irrelevant edges from the graph.
     */
    public final static String RELEVANTEDGES = "Relevant Edges";
    /**
     * Identifier for the filter that only keeps repropagated information by other users.
     */
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
