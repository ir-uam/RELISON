/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.sna.pair;

/**
 * Identifiers for metrics for pairs of users in the network.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class PairMetricIdentifiers 
{
    /**
     * Identifier for the Clustering Coefficient increment metric.
     */
    public static final String CCINCREASE = "Clustering coefficient increment";
    /**
     * Identifier for the distance between pairs metric.
     */
    public static final String DISTANCE = "Distance";
    /**
     * Identifier for the distance without a link metric.
     */
    public static final String DISTANCEWITHOUTLINK = "Distance without link";
    /**
     * Identifier for the reciprocal shortest path length metric.
     */
    public static final String RECIPROCALSPL = "Reciprocal shortest path length";
    /**
     * Identifier for the neighbor overlap metric.
     */
    public static final String FOAF = "Neighbor overlap";
    /**
     * Identifier for the neighbor overlap metric in the complementary graph.
     */
    public static final String COMPLFOAF = "Complementary neighbor overlap";

    /**
     * Identifier for the number of geodesic paths metric.
     */
    public static final String GEODESICS = "Geodesics";
    /**
     * Identifier for the reciprocity metric.
     */
    public static final String RECIP = "Reciprocity";

    /**
     * Identifier for the weighted neighbor overlap metric.
     */
    public static final String WFOAF = "Weighted neighbor overlap";
    /**
     * Identifier for the log-weighted neighbor overlap metric.
     */
    public static final String WFOAFLOG = "Log weighted neighbor overlap";

    /**
     * Identifier for the expanded common neighbors metric.
     */
    public static final String EFOAF = "Expanded common neighbors";
    /**
     * Identifier for the expanded common neighbors count metric.
     */
    public static final String EFOAFCOUNT = "Expanded common neighbors count";
    /**
     * Identifier for the preferential attachment metric.
     */
    public static final String PREFATTACH = "Preferential attachment";
    /**
     * Identifier for the embeddedness metric.
     */
    public static final String EMBEDEDNESS = "Embeddedness";
    /**
     * Identifier for the embeddedness metric in the complementary graph.
     */
    public static final String COMPLEMBEDEDNESS = "Complementary embeddedness";
    /**
     * Identifier for the Clustering Coefficient increment weakness metric.
     */
    public static final String WEAKNESS = "Weakness";
    /**
     * Identifier for the shrinking ASL metric.
     */
    public static final String SHRINKINGASL = "Shrinking ASL";
    /**
     * Identifier for the shrinking diameter metric.
     */
    public static final String SHRINKINGDIAM = "Shrinking diameter";
    /**
     * Identifier for the shrinking neighbors diameter metric.
     */
    public static final String SHRINKINGDIAMNEIGH = "Shrinking neighbors diameter";
    /**
     * Identifier for the shrinking neighbors ASL metric.
     */
    public static final String SHRINKINGASLNEIGH = "Shrinking neighbors ASL";
    /**
     * Identifier for the betweenness metric.
     */
    public static final String BETWEENNESS = "Betweenness";
    /**
     * Identifier for the weight of pairs.
     */
    public static final String WEIGHT = "Weight";

    
}
