/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.sna.vertex;

/**
 * Identifiers for vertex metrics.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class VertexMetricIdentifiers 
{
    public final static String CLOSENESS = "Closeness";
    public final static String BETWEENNESS = "Betweenness";
    public final static String ECCENTRICITY = "Eccentricity";
    public final static String HARMONIC = "Harmonic";
    
    public final static String DEGREE = "Degree";
    public final static String HITS = "HITS";
    public final static String INVDEGREE = "Inverse degree";
    public final static String LOCALCLUSTCOEF = "Local clustering coefficient";
    public final static String LOCALRECIPRATE = "Reciprocity rate";
    public final static String PAGERANK = "PageRank";
    
    public final static String COMPLDEGREE = "Complementary degree";
    public final static String COMPLINVDEGREE = "Complementary inverse degree";
    public final static String COMPLLOCALCLUSTCOEF = "Complementary local clustering coefficient";
    public final static String COMPLPAGERANK = "Complementary PageRank";
    
    public final static String FD = "Free discovery";
    public final static String TERMNORM = "Term normalization";
    public final static String LENGTH = "Length";

    public final static String CORENESS = "Coreness";
    public final static String EIGEN = "Eigenvector";
    public final static String KATZ = "Katz";

}
