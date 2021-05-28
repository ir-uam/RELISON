/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.community;

/**
 * Identifiers for the different community detection algorithms available in the framework.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class CommunityDetectionIdentifiers 
{
    // Connectedness
    public final static String SCC = "Strongly connected components";
    public final static String WCC = "Weakly Connected Components";
    
    // Modularity optimizing community detection algorithms
    public final static String FASTGREEDY = "FastGreedy";
    public final static String LOUVAIN = "Louvain";
    public final static String INFOMAP = "Infomap";
    public final static String LABELPROP = "Label propagation";

    // Community detection algorithms using edge metrics
    public final static String GIRVANNEWMAN = "Girvan Newman";
    public final static String EDGEBETWENNESS = "Edge betweenness";
    
    // Balanced community detection algorithms
    public final static String BALANCEDFASTGREEDY = "Balanced FastGreedy";
    public final static String SIZEWEIGHTEDFASTGREEDY = "Size Weighted FastGreedy";
    public final static String GINIWEIGHTEDFASTGREEDY = "Gini Weighted FastGreedy";
    public final static String RATIOCUTSPECTRAL = "Ratio cut spectral clustering";
    public final static String NORMALIZEDCUTSPECTRAL = "Normalized cut spectral clustering";
    
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
        System.out.println("\t" + LABELPROP);
        System.out.println();

        System.out.println("Edge metric-based algorithms:");
        System.out.println("\t" + GIRVANNEWMAN + " (or " + EDGEBETWENNESS + ")");
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
