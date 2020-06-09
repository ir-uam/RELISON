/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms;

/**
 * Identifiers for the different contact recommendation algorithms available in
 * the library
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class AlgorithmIdentifiers
{
    // IR algorithms
    public final static String BIR = "BIR";
    public final static String BM25 = "BM25";
    public final static String EBM25 = "EBM25";

    public final static String QLJM = "QLJM";
    public final static String QLD = "QLD";
    public final static String QLL = "QLL";

    public final static String VSM = "VSM";

    public final static String PL2 = "PL2";
    public final static String DLH = "DLH";
    public final static String DPH = "DPH";
    public final static String DFREE = "DFRee";
    public final static String DFREEKLIM = "DFReeKLIM";

    // Friends of friends
    public final static String ADAMIC = "Adamic";
    public final static String JACCARD = "Jaccard";
    public final static String MCN = "MCN";
    public final static String COSINE = "Cosine";
    public final static String HPI = "Hub promoted index";
    public final static String HDI = "Hub depressed index";
    public final static String SORENSEN = "Sorensen";
    public final static String LOCALLHN = "Local LHN";
    public final static String RESALLOC = "Resource allocation";

    // Distances
    public final static String DISTANCE = "Distance";
    // Random walks
    public final static String PAGERANK = "PageRank";
    public final static String SALSA = "PageRank";
    public final static String HITS = "HITS";

    public final static String PERSPAGERANK = "Personalized PageRank";
    public final static String PERSSALSA = "Personalized SALSA";
    public final static String PERSHITS = "Personalized HITS";

    public final static String PROPFLOW = "PropFlow";

    // Twitter
    public final static String CLOSURE = "Closure";
    public final static String MONEY = "Money";
    public final static String LOVE = "Love";
    public final static String TWITTERAVGCOS = "Twitter average cosine";
    public final static String TWITTERCENTROIDCOS = "Twitter centroid cosine";
    public final static String TWITTERMAXCOS = "Twitter maximum cosine";


    // Collaborative filtering
    public final static String IMF = "iMF";
    public final static String UB = "UB kNN";
    public final static String IB = "IB kNN";

    public final static String POP = "Popularity";
    public final static String RANDOM = "Random";
}
