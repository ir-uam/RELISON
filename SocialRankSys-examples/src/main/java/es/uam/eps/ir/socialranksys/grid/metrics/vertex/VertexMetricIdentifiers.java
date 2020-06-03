/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.metrics.vertex;

/**
 * Identifiers for vertex metrics
 * @author Javier Sanz-Cruzado Puig
 */
public class VertexMetricIdentifiers 
{
    public final static String CLOSENESS = "Closeness";
    public final static String BETWEENNESS = "Betweenness";
    public final static String ECCENTRICITY = "Eccentricity";
    
    public final static String DEGREE = "Degree";
    public final static String HITS = "HITS";
    public final static String INVDEGREE = "Inverse Degree";
    public final static String LOCALCLUSTCOEF = "Local Clustering Coefficient";
    public final static String LOCALRECIPRATE = "Reciprocity Rate";
    public final static String PAGERANK = "PageRank";
    
    public final static String COMPLDEGREE = "Complementary Degree";
    public final static String COMPLINVDEGREE = "Complementary Inverse Degree";
    public final static String COMPLLOCALCLUSTCOEF = "Complementary Local Clustering Coefficient";
    public final static String COMPLPAGERANK = "Complementary PageRank";
    
    public final static String FD = "Free Discovery";
    public final static String TERMNORM = "Term Normalization";
    public final static String LENGTH = "Length";

}
