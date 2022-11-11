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
    /**
     * Identifier for the closeness metric.
     */
    public final static String CLOSENESS = "Closeness";
    /**
     * Identifier for the betweenness metric.
     */
    public final static String BETWEENNESS = "Betweenness";
    /**
     * Identifier for the eccentricity metric.
     */
    public final static String ECCENTRICITY = "Eccentricity";
    /**
     * Identifier for the harmonic centrality metric.
     */
    public final static String HARMONIC = "Harmonic";
    /**
     * Identifier for the degree.
     */
    public final static String DEGREE = "Degree";
    /**
     * Identifier for the HITS score.
     */
    public final static String HITS = "HITS";
    /**
     * Identifier for the inverse of the degree.
     */
    public final static String INVDEGREE = "Inverse degree";
    /**
     * Identifier for the local clustering coefficient.
     */
    public final static String LOCALCLUSTCOEF = "Local clustering coefficient";
    /**
     * Identifier for the reciprocity rate.
     */
    public final static String LOCALRECIPRATE = "Reciprocity rate";
    /**
     * Identifier for the PageRank
     */
    public final static String PAGERANK = "PageRank";

    /**
     * Identifier for the degree in the complementary graph.
     */
    public final static String COMPLDEGREE = "Complementary degree";
    /**
     * Identifier for the inverse degree in the complementary graph.
     */
    public final static String COMPLINVDEGREE = "Complementary inverse degree";
    /**
     * Identifier for the local clustering coefficient in the complementary graph.
     */
    public final static String COMPLLOCALCLUSTCOEF = "Complementary local clustering coefficient";
    /**
     * Identifier for the PageRank in the complementary graph.
     */
    public final static String COMPLPAGERANK = "Complementary PageRank";

    /**
     * Identifier for the Free discovery metric.
     */
    public final static String FD = "Free discovery";
    /**
     * Identifier for the length of the node.
     */
    public final static String LENGTH = "Length";

    /**
     * Identifier for the coreness metric.
     */
    public final static String CORENESS = "Coreness";
    /**
     * Identifier for the eigenvector centrality.
     */
    public final static String EIGEN = "Eigenvector";
    /**
     * Identifier for the Katz centrality.
     */
    public final static String KATZ = "Katz";

}
