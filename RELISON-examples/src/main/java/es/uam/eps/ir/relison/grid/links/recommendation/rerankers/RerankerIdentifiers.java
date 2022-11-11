/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.links.recommendation.rerankers;

/**
 * Identifiers for the different contact recommendation algorithms available in 
 * the library
 * @author Javier Sanz-Cruzado Puig
 */
public class RerankerIdentifiers 
{
    // Random reranker
    /**
     * Identifier for the random reranker.
     */
    public final static String RANDOM = "Random";
    
    // Local algorithms (Classic lambda greedy rerankers)
    /**
     * Identifier for the reranker incrementing the number of weak ties.
     */
    public final static String WEAKTIES = "WeakTies";
    /**
     * Identifier for the reranker incrementing the number of strong ties.
     */
    public final static String STRONGTIES = "StrongTies";

    /**
     * Identifier for the local reranker promoting a user metric.
     */
    public final static String LOCALUSERMETRIC = "LocalUserMetric";
    /**
     * Identifier for the local reranker promoting an edge metric.
     */
    public final static String LOCALEDGEMETRIC = "LocalEdgeMetric";
    /**
     * Identifier for the local reranker promoting a graph metric.
     */
    public final static String LOCALGRAPHMETRIC = "LocalGraphMetric";
    /**
     * Identifier for the local reranker promoting a community metric.
     */
    public final static String LOCALCOMMMETRIC = "LocalCommMetric";
    /**
     * Identifier for the local reranker promoting an individual community metric.
     */
    public final static String LOCALAVGCOMMMETRIC = "LocalIndivCommMetric";
    
    // Swap rerankers
    /**
     * Identifier for the swap reranker promoting clustering coefficient.
     */
    public final static String CLUSTCOEF = "ClusteringCoefficient";
    /**
     * Identifier for the swap reranker promoting clustering coefficient complement.
     */
    public final static String CLUSTCOEFCOMPL = "ClusteringCoefficientComplement";
    /**
     * Identifier for the swap reranker promoting degree Gini complement.
     */
    public final static String DEGREEGINICOMPL = "DegreeGiniComplement";
    /**
     * Identifier for the swap reranker promoting average embeddedness.
     */
    public final static String AVGEMBEDDEDNESS = "AverageEmbeddedness";
    /**
     * Identifier for the swap reranker promoting average weakness.
     */
    public final static String AVGWEAKNESS = "AverageWeakness";
    /**
     * Identifier for the swap reranker promoting average embeddedness with heuristics.
     */
    public final static String HEURISTICAVGEMBEDDEDNESS = "HeuristicAverageEmbeddedness";
    /**
     * Identifier for the swap reranker promoting average weakness with heuristics.
     */
    public final static String HEURISTICAVGWEAKNESS = "HeuristicAverageWeakness";
    /**
     * Identifier for the swap reranker promoting inter-community degree Gini complement.
     */
    public final static String ICDEGREEGINI = "InterCommunityDegree-GiniComplement";
    /**
     * Identifier for the swap reranker promoting inter-community degree Gini complement, forcing edges out of the
     * community.
     */
    public final static String OUTERICDEGREEGINI = "Outer-InterCommunityDegree-GiniComplement";
    /**
     * Identifier for the swap reranker promoting inter-community degree Gini complement.
     */
    public final static String CDEGREEGINI = "CompleteCommunityDegree-GiniComplement";
    /**
     * Identifier for the swap reranker promoting complete community degree Gini, forcing edges out of the community.
     */
    public final static String OUTERCDEGREEGINI = "Outer-CompleteCommunityDegree-GiniComplement";

    /**
     * Identifier for the swap reranker promoting size-normalized inter-community degree Gini complement.
     */
    public final static String SNICDEGREEGINI = "SizeNorm-InterCommunityDegree-GiniComplement";
    /**
     * Identifier for the swap reranker promoting size-normalized inter-community degree Gini complement, forcing edges out
     * of the community.
     */
    public final static String OUTERSNICDEGREEGINI = "Outer-SizeNorm-InterCommunityDegree-GiniComplement";
    /**
     * Identifier for the swap reranker promoting the size-normalized complete community degree Gini complement.
     */
    public final static String SNCDEGREEGINI = "SizeNorm-CompleteCommunityDegree-GiniComplement";
    /**
     * Identifier for the swap reranker promoting the size-normalized complete community degree Gini complement, forcing edges
     * out of the community.
     */
    public final static String OUTERSNCDEGREEGINI = "Outer-SizeNorm-CompleteCommunityDegree-GiniComplement";

    /**
     * Identifier for the swap reranker promoting the inter-community edge Gini complement.
     */
    public final static String ICEDGEGINI = "InterCommunityEdge-GiniComplement";
    /**
     * Identifier for the swap reranker promoting the inter-community edge Gini complement, forcing edges out of the community.
     */
    public final static String OUTERICEDGEGINI = "Outer-InterCommunityEdge-GiniComplement";
    /**
     * Identifier for the swap reranker promoting the semi-complete community edge Gini complement.
     */
    public final static String SCEDGEGINI = "SemiCompleteCommunityEdge-GiniComplement";
    /**
     * Identifier for the swap reranker promoting the semi-complete community edge Gini complement, forcing edges out of the community.
     */
    public final static String OUTERSCEDGEGINI = "Outer-SemiCompleteCommunityEdge-GiniComplement";
    /**
     * Identifier for the swap reranker promoting the complete community edge Gini complement.
     */
    public final static String CEDGEGINI = "CompleteCommunityEdge-GiniComplement";
    /**
     * Identifier for the swap reranker promoting the complete community edge Gini complement, forcing edges out of the community.
     */
    public final static String OUTERCEDGEGINI = "Outer-CompleteCommunityEdge-GiniComplement";
    /**
     * Identifier for an alternative swap reranker promoting the semi-complete community edge Gini complement.
     */
    public final static String ALTSCEDGEGINI = "AltSemiCompleteCommunityEdge-GiniComplement";
    /**
     * Identifier for the size-normalized inter-community edge Gini complement.
     */
    public final static String SNICEDGEGINI = "SizeNorm-InterCommunityEdge-GiniComplement";
    /**
     * Identifier for the size-normalized inter-community edge Gini complement, forcing edges out of the community.
     */
    public final static String OUTERSNICEDGEGINI = "Outer-SizeNorm-InterCommunityEdge-GiniComplement";
    /**
     * Identifier for the size-normalized semi-complete community edge Gini complement.
     */
    public final static String SNSCEDGEGINI = "SizeNorm-SemiCompleteCommunityEdge-GiniComplement";
    /**
     * Identifier for the size-normalized semi-complete community edge Gini complement, forcing edges out of the community.
     */
    public final static String OUTERSNSCEDGEGINI = "Outer-SizeNorm-SemiCompleteCommunityEdge-GiniComplement";
    /**
     * Identifier for the size-normalized complete community edge Gini complement.
     */
    public final static String SNCEDGEGINI = "SizeNorm-CompleteCommunityEdge-GiniComplement";
    /**
     * Identifier for the size-normalized complete community edge Gini complement, forcing edges out of the community.
     */
    public final static String OUTERSNCEDGEGINI = "Outer-SizeNorm-CompleteCommunityEdge-GiniComplement";
    /**
     * Identifier for an alternative swap reranker promoting the semi-complete community edge Gini complement, forcing edges
     * out of the community.
     */
    public final static String ALTOUTERSCEDGEGINI = "AltOuter-SemiCompleteCommunityEdge-GiniComplement";
    /**
     * Identifier for the generic swap reranker for user metrics.
     */
    public final static String SWAPUSERMETRIC = "SwapUserMetric";
    /**
     * Identifier for the generic swap reranker for edge metrics.
     */
    public final static String SWAPEDGEMETRIC = "SwapEdgeMetric";
    /**
     * Identifier for the generic swap reranker for graph metrics.
     */
    public final static String SWAPGRAPHMETRIC = "SwapGraphMetric";
    /**
     * Identifier for the generic swap reranker for community metrics.
     */
    public final static String SWAPCOMMMETRIC = "SwapCommMetric";
    /**
     * Identifier for the generic swap reranker for individual community metrics.
     */
    public final static String SWAPAVGCOMMMETRIC = "SwapIndivCommMetric";
    
    
    /**
     * Prints the list of available algorithms
     */
    public static void printAlgorithmList()
    {
        System.out.println("Local rerankers:");
        System.out.println("\t\t" + RANDOM);
        
        System.out.println("\tSpecific algorithms:");
        // Local algorithms (Classic lambda greedy rerankers)
        System.out.println("\t\t" + WEAKTIES);
        System.out.println("\t\t" + STRONGTIES);

        System.out.println("\tGeneral algorithms:");
        System.out.println("\t\t" + LOCALUSERMETRIC);
        System.out.println("\t\t" + LOCALEDGEMETRIC);
        System.out.println("\t\t" + LOCALGRAPHMETRIC);
        System.out.println("\t\t" + LOCALCOMMMETRIC);
        System.out.println("\t\t" + LOCALAVGCOMMMETRIC);

        System.out.println("\n");
        // Swap rerankers
        System.out.println("Swap rerankers");
        
        System.out.println("\tSpecific algorithms:");
        System.out.println("\t\tClustering Coefficient rerankers:");
        System.out.println("\t\t\t" + CLUSTCOEF);
        System.out.println("\t\t\t" + CLUSTCOEFCOMPL);
        System.out.println("\t\tDegree Gini coefficient rerankers:");
        System.out.println("\t\t\t" + DEGREEGINICOMPL);
        System.out.println("\t\tAverage Edge Neighbor Overlap rerankers:");
        System.out.println("\t\t\t" + AVGEMBEDDEDNESS);
        System.out.println("\t\t\t" + AVGWEAKNESS);
        System.out.println("\t\t\t" + HEURISTICAVGEMBEDDEDNESS);
        System.out.println("\t\t\t" + HEURISTICAVGWEAKNESS);
        System.out.println("\t\tCommunity Degree Gini rerankers:");
        System.out.println("\t\t\t" + ICDEGREEGINI);
        System.out.println("\t\t\t" + OUTERICDEGREEGINI);
        System.out.println("\t\t\t" + CDEGREEGINI);
        System.out.println("\t\t\t" + OUTERCDEGREEGINI);
        System.out.println("\t\tSize-Normalized Community Degree Gini rerankers:");
        System.out.println("\t\t\t" + SNICDEGREEGINI);
        System.out.println("\t\t\t" + OUTERSNICDEGREEGINI);
        System.out.println("\t\t\t" + SNCDEGREEGINI);
        System.out.println("\t\t\t" + OUTERSNCDEGREEGINI);
        System.out.println("\t\tCommunity Edge Gini rerankers:");
        System.out.println("\t\t\t" + ICEDGEGINI);
        System.out.println("\t\t\t" + OUTERICEDGEGINI);
        System.out.println("\t\t\t" + SCEDGEGINI);
        System.out.println("\t\t\t" + OUTERSCEDGEGINI); 
        System.out.println("\t\t\t" + CEDGEGINI);
        System.out.println("\t\t\t" + OUTERCEDGEGINI);
        System.out.println("\t\tSize-Normalized Community Edge Gini rerankers:");
        System.out.println("\t\t\t" + SNICEDGEGINI);
        System.out.println("\t\t\t" + OUTERSNICEDGEGINI);
        System.out.println("\t\t\t" + SNSCEDGEGINI);
        System.out.println("\t\t\t" + OUTERSNSCEDGEGINI); 
        System.out.println("\t\t\t" + SNCEDGEGINI);
        System.out.println("\t\t\t" + OUTERSNCEDGEGINI);
        System.out.println("\tGeneral algorithms:");
        System.out.println("\t\t" + SWAPUSERMETRIC);
        System.out.println("\t\t" + SWAPEDGEMETRIC);
        System.out.println("\t\t" + SWAPGRAPHMETRIC);
        System.out.println("\t\t" + SWAPCOMMMETRIC);
        System.out.println("\t\t" + SWAPAVGCOMMMETRIC);
    }
}
