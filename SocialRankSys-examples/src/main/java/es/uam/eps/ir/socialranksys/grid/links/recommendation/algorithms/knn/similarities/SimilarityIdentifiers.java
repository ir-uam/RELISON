/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.knn.similarities;

/**
 * Identifiers for the different user to user similarities
 * available in the library.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class SimilarityIdentifiers 
{
    public static final String VECTORCOSINE = "cosine";
    public static final String JACCARD = "jaccard";
    public static final String MCN = "mcn";
    public static final String ADAMIC = "adamic";
    public static final String VSM = "vsm";
    public static final String BM25 = "bm25";
    public static final String EBM25 = "extremebm25";
    public static final String BIR = "bir";
    public static final String QLJM = "qljm";
    public static final String QLD = "qld";
    public static final String QLL = "qll";
    
    public static final String PL2 = "pl2";
    public static final String DLH = "dlh";
    public static final String DPH = "dph";
    public static final String DFREE = "dfree";
    public static final String DFREEKLIM = "dfreeklim";

}
