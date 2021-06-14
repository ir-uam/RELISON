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
    public static final String CCINCREASE = "Clustering coefficient increment";

    public static final String DISTANCE = "Distance";
    public static final String DISTANCEWITHOUTLINK = "Distance without link";
    public static final String RECIPROCALSPL = "Reciprocal shortest path length";

    public static final String FOAF = "Neighbor overlap";
    public static final String COMPLFOAF = "Complementary neighbor overlap";


    public static final String GEODESICS = "Geodesics";
    public static final String RECIP = "Reciprocity";


    public static final String WFOAF = "Weighted neighbor overlap";
    public static final String WFOAFLOG = "Log weighted neighbor overlap";
    public static final String EFOAF = "Expanded common neighbors";
    public static final String EFOAFCOUNT = "Expanded common neighbors count";
    public static final String PREFATTACH = "Preferential attachment";
    
    public static final String EMBEDEDNESS = "Embeddedness";
    public static final String COMPLEMBEDEDNESS = "Complementary embeddedness";
    public static final String WEAKNESS = "Weakness";

    public static final String SHRINKINGASL = "Shrinking ASL";
    public static final String SHRINKINGDIAM = "Shrinking diameter";
    public static final String SHRINKINGDIAMNEIGH = "Shrinking neighbors diameter";
    public static final String SHRINKINGASLNEIGH = "Shrinking neighbors ASL";

    public static final String BETWEENNESS = "Betweenness";
    public static final String WEIGHT = "Weight";

    
}
