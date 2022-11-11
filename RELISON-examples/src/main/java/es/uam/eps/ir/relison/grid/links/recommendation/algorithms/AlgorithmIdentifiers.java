/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.links.recommendation.algorithms;

/**
 * Identifiers for the different contact recommendation algorithms available in
 * the library.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class AlgorithmIdentifiers
{
    // IR algorithms

    /**
     * Identifier for the Binary Independent Retrieval (BIR) algorithm.
     */
    public final static String BIR = "BIR";
    /**
     * Identifier for the BM25 algorithm.
     */
    public final static String BM25 = "BM25";
    /**
     * Identifier for the Extreme BM25 algorithm.
     */
    public final static String EBM25 = "EBM25";
    /**
     * Identifier for the Query Likelihood with Jelinek-Mercer smoothing algorithm.
     */
    public final static String QLJM = "QLJM";
    /**
     * Identifier for the Query Likelihood with Dirichlet smoothing algorithm.
     */
    public final static String QLD = "QLD";
    /**
     * Identifier for the Query Likelihood with Laplace smoothing algorithm.
     */
    public final static String QLL = "QLL";
    /**
     * Identifier for the Vector Space Model algorithm.
     */
    public final static String VSM = "VSM";
    /**
     * Identifier for the pivoted normalization Vector Space Model algorithm.
     */
    public final static String PIVOTEDVSM = "Pivoted normalization VSM";
    /**
     * Identifier for the PL2 algorithm.
     */
    public final static String PL2 = "PL2";
    /**
     * Identifier for the DLH algorithm.
     */
    public final static String DLH = "DLH";
    /**
     * Identifier for the DPH algorithm.
     */
    public final static String DPH = "DPH";
    /**
     * Identifier for the DFRee algorithm.
     */
    public final static String DFREE = "DFRee";
    /**
     * Identifier for the DFRee KLIM algorithm.
     */
    public final static String DFREEKLIM = "DFReeKLIM";

    // Friends of friends
    /**
     * Identifier for the Adamic-Adar algorithm.
     */
    public final static String ADAMIC = "Adamic-Adar";
    /**
     * Identifier for the Jaccard algorithm.
     */
    public final static String JACCARD = "Jaccard";
    /**
     * Identifier for the Most Common Neighbors (MCN) algorithm.
     */
    public final static String MCN = "MCN";
    /**
     * Identifier for the Cosine similarity algorithm.
     */
    public final static String COSINE = "Cosine";
    /**
     * Identifier for the Hub Promoted Index algorithm.
     */
    public final static String HPI = "Hub promoted index";
    /**
     * Identifier for the Hub Depressed Index algorithm.
     */
    public final static String HDI = "Hub depressed index";
    /**
     * Identifier for the Sorensen similarity algorithm.
     */
    public final static String SORENSEN = "Sorensen";
    /**
     * Identifier for the local Leicht-Holme-Newman algorithm.
     */
    public final static String LOCALLHN = "Local LHN";
    /**
     * Identifier for the Resource Allocation algorithm.
     */
    public final static String RESALLOC = "Resource allocation";

    // Distances
    /**
     * Identifier for the distance algorithm.
     */
    public final static String DISTANCE = "Distance";

    // Path-based
    /**
     * Identifier for the Katz algorithm.
     */
    public static final String KATZ = "Katz";
    /**
     * Identifier for the Local Path Index algorithm.
     */
    public static final String LPI = "Local path index";
    /**
     * Identifier for the global Leicht-Holme-Newman  algorithm.
     */
    public static final String GLOBALLHN = "Global LHN";
    /**
     * Identifier for the Matrix Forest algorithm.
     */
    public static final String MATRIXFOREST = "Matrix forest";
    /**
     * Identifier for the Pseudo-Inverse Cosine similarity algorithm.
     */
    public static final String PIC = "Pseudo-inverse cosine";

    // Random walks
    /**
     * Identifier for the PageRank algorithm.
     */
    public final static String PAGERANK = "PageRank";
    /**
     * Identifier for the SALSA algorithm.
     */
    public final static String SALSA = "SALSA";
    /**
     * Identifier for the HITS algorithm.
     */
    public final static String HITS = "HITS";
    /**
     * Identifier for the Personalized PageRank algorithm.
     */
    public final static String PERSPAGERANK = "Personalized PageRank";
    /**
     * Identifier for the Personalized SALSA algorithm.
     */
    public final static String PERSSALSA = "Personalized SALSA";
    /**
     * Identifier for the Personalized HITS algorithm.
     */
    public final static String PERSHITS = "Personalized HITS";
    /**
     * Identifier for the commute time (PageRank-based) algorithm.
     */
    public final static String COMMUTE = "Commute time PageRank";
    /**
     * Identifier for the hitting time (PageRank-based) algorithm.
     */
    public final static String HITTING = "Hitting time PageRank";
    /**
     * Identifier for the commute time (personalized PageRank-based) algorithm.
     */
    public final static String COMMUTEPERS = "Commute time personalized PageRank";
    /**
     * Identifier for the hitting time (personalized PageRank-based) algorithm.
     */
    public final static String HITTINGPERS = "Hitting time personalized PageRank";
    /**
     * Identifier for the PropFlow algorithm.
     */
    public final static String PROPFLOW = "PropFlow";

    // Twitter
    /**
     * Identifier for the Closure algorithm.
     */
    public final static String CLOSURE = "Closure";
    /**
     * Identifier for the Money algorithm.
     */
    public final static String MONEY = "Money";
    /**
     * Identifier for the Love algorithm.
     */
    public final static String LOVE = "Love";
    /**
     * Identifier for the Average Cosine similarity algorithm by Twitter.
     */
    public final static String TWITTERAVGCOS = "Average cosine";
    /**
     * Identifier for the Centroid Cosine similarity algorithm by Twitter.
     */
    public final static String TWITTERCENTROIDCOS = "Centroid cosine";
    /**
     * Identifier for the Maximum Cosine similarity algorithm by Twitter.
     */
    public final static String TWITTERMAXCOS = "Maximum cosine";

    // Collaborative filtering
    /**
     * Identifier for the implicit Matrix Factorization algorithm.
     */
    public final static String IMF = "iMF";
    /**
     * Identifier for the fast implicit Matrix Factorization algorithm.
     */
    public final static String FASTIMF = "Fast iMF";
    /**
     * Identifier for the User-based k-nearest neighbors algorithm.
     */
    public final static String UB = "UB kNN";
    /**
     * Identifier for the Item-based k-nearest neighbors algorithm.
     */
    public final static String IB = "IB kNN";

    /**
     * Identifier for the Popularity-based algorithm
     */
    public final static String POP = "Popularity";
    /**
     * Identifier for the Random algorithm
     */
    public final static String RANDOM = "Random";

    // Content-based
    /**
     * Identifier for the centroid content-based algorithm
     */
    public final static String CENTROIDCB = "centroidCB";
    /**
     * Identifier for the Twittomender algorithm
     */
    public final static String TWITTOMENDER = "Twittomender";

    // Supervised
    /**
     * Identifier for the LambdaMART algorithm
     */
    public final static String LAMBDAMART = "lambdaMART";
    /**
     * Identifier for the Weka-based algorithms
     */
    public final static String WEKA = "Weka";
}
