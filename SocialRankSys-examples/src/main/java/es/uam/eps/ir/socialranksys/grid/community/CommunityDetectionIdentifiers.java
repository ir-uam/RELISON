/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.community;

/**
 * Identifiers for the different community detection algorithms available in 
 * the library.
 * @author Javier Sanz-Cruzado Puig
 */
public class CommunityDetectionIdentifiers 
{
    // Connectedness
    public final static String SCC = "Strongly Connected Components";
    public final static String WCC = "Weakly Connected Components";
    
    // Modularity optimizing community detection algorithms
    public final static String FASTGREEDY = "FastGreedy";
    public final static String LOUVAIN = "Louvain";
    public final static String INFOMAP = "Infomap";
    
    // Balanced community detection algorithms
    public final static String BALANCEDFASTGREEDY = "Balanced FastGreedy";
    public final static String SIZEWEIGHTEDFASTGREEDY = "Size Weighted FastGreedy";
    public final static String GINIWEIGHTEDFASTGREEDY = "Gini Weighted FastGreedy";
    public final static String RATIOCUTSPECTRAL = "Ratio Cut Spectral Clustering";
    public final static String NORMALIZEDCUTSPECTRAL = "Normalized Cut Spectral Clustering";
    
    // Modularity-based community detection algorithms
    //public static String FASTGREEDY = "FastGreedy";
    
    /**
     * Prints the list of available algorithms
     */
    public static void printAlgorithmList()
    {
        System.out.println("Algorithms based on the connectedness of the network:");
        System.out.println("\t" + SCC);
        System.out.println("\t" + WCC);
        System.out.println();
        
        System.out.println("Modularity-based algorithms:");
        System.out.println("\t" + FASTGREEDY);
        System.out.println("\t" + LOUVAIN);
        System.out.println("\t" + INFOMAP);
        System.out.println();
        
        System.out.println("Balanced community size algorithms based on modularity:");
        System.out.println("\t" + BALANCEDFASTGREEDY);
        System.out.println("\t" + SIZEWEIGHTEDFASTGREEDY);
        System.out.println("\t" + GINIWEIGHTEDFASTGREEDY);
        System.out.println("\t" + RATIOCUTSPECTRAL);
        System.out.println("\t" + NORMALIZEDCUTSPECTRAL);
        System.out.println();
        
    }
}
