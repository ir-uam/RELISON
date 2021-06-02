/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.sna.graph;

/**
 * Identifiers for edge metrics.
 * @author Javier Sanz-Cruzado Puig
 */
public class GraphMetricIdentifiers 
{
    public static final String ASL = "ASL";
    public static final String ARSL = "ARSL";
    public static final String RADIUS = "Radius";
    public static final String DIAMETER = "Diameter";
    public static final String INFINITEDIST = "Infinite distances";

    public static final String CLUSTCOEF = "Clustering coefficient";
    public static final String CLUSTCOEFCOMPL = "Clustering coefficient complement";

    public static final String DEGREEGINI = "Degree Gini complement";
    public static final String DENSITY = "Density";
    public static final String NUMEDGES = "Num. Edges";

    public static final String INTEREDGEGINI = "Inter edge Gini complement";
    public static final String COMPLETEEDGEGINI = "Complete edge Gini complement";
    public static final String SEMICOMPLETEEDGEGINI = "Semi-complete edge Gini complement";


    public static final String RECIPRAVGECCENTRICITY = "Reciprocal average eccentricity";
    public static final String RECIPRDIAMETER = "Reciprocal diameter";

    public static final String RECIPROCITYRATE = "Reciprocity";

    public static final String DEGREEASSORT = "Degree assortativity";
    public static final String DEGREEPEARSON = "Degree Pearson";
}
