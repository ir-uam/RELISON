/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.sna.comm.indiv;

/**
 * Identifiers for individual community metrics.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class IndividualCommunityMetricIdentifiers 
{
    /**
     * Identifier for the community degree metric.
     */
    public static final String COMMDEGREE = "Degree";
    /**
     * Identifier for the community size metric.
     */
    public static final String COMMSIZE = "Size";
    /**
     * Identifier for the volume metric.
     */
    public static final String VOLUME = "Volume";
    /** Individual community boundary, retention and quality metric identifiers. */
    public static final String BOUNDARYNODES = "Boundary nodes";
    public static final String BOUNDARYNODERATIO = "Boundary-node ratio";
    public static final String NEIGHBORINGCOMMUNITIES = "Distinct neighboring communities";
    public static final String INTERNALVOLUMEFRACTION = "Internal volume fraction";
    public static final String CONDUCTANCE = "Conductance";
    public static final String EDGEEXPANSION = "Edge expansion";
    public static final String MODULARITYCONTRIBUTION = "Modularity contribution (Newman)";
    
}
