/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.sna.graph;

/**
 * Identifiers for edge metrics.
 * @author Javier Sanz-Cruzado Puig
 */
public class GraphMetricIdentifiers 
{
    /**
     * Identifier for average shortest path length metric.
     */
    public static final String ASL = "ASL";
    /**
     * Identifier for average reciprocal shortest path length metric.
     */
    public static final String ARSL = "ARSL";
    /**
     * Identifier for radius metric.
     */
    public static final String RADIUS = "Radius";
    /**
     * Identifier for diameter metric.
     */
    public static final String DIAMETER = "Diameter";
    /**
     * Identifier for number of pairs at infinite distances metric.
     */
    public static final String INFINITEDIST = "Infinite distances";
    /**
     * Identifier for global clustering coefficient metric.
     */
    public static final String CLUSTCOEF = "Clustering coefficient";
    /**
     * Identifier for global clustering coefficient complement metric.
     */
    public static final String CLUSTCOEFCOMPL = "Clustering coefficient complement";
    /**
     * Identifier for degree Gini metric.
     */
    public static final String DEGREEGINI = "Degree Gini complement";
    /**
     * Identifier for density metric.
     */
    public static final String DENSITY = "Density";
    /**
     * Identifier for number of edges metric.
     */
    public static final String NUMEDGES = "Num. Edges";

    /**
     * Identifier for inter-edge Gini complement metric.
     */
    public static final String INTEREDGEGINI = "Inter edge Gini complement";
    /**
     * Identifier for complete edge Gini complement metric.
     */
    public static final String COMPLETEEDGEGINI = "Complete edge Gini complement";
    /**
     * Identifier for semi-complete edge Gini complement.
     */
    public static final String SEMICOMPLETEEDGEGINI = "Semi-complete edge Gini complement";

    /**
     * Identifier for reciprocal average eccentricity metric.
     */
    public static final String RECIPRAVGECCENTRICITY = "Reciprocal average eccentricity";
    /**
     * Identifier for reciprocal diameter metric.
     */
    public static final String RECIPRDIAMETER = "Reciprocal diameter";
    /**
     * Identifier for reciprocity rate metric.
     */
    public static final String RECIPROCITYRATE = "Reciprocity";
    /**
     * Identifier for degree assortativity metric.
     */
    public static final String DEGREEASSORT = "Degree assortativity";
    /**
     * Identifier for degree Pearson correlation metric.
     */
    public static final String DEGREEPEARSON = "Degree Pearson";
}
