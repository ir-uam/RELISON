/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.data;

/**
 * Identifiers for the different types of communities.
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class CommunitiesIdentifiers
{
    /**
     * Identifier for Strongly Connected Components
     */
    public static final String SCC = "SCC";
    /**
     * Identifier for Infomap comm. detection algorithm.
     */
    public static final String INFOMAP = "Infomap";
    /**
     * Identifier for Louvain comm. detection algorithm.
     */
    public static final String BLONDEL = "Blondel";
    /**
     * Identifier for Newman comm. detection algorithm.
     */
    public static final String NEWMAN = "Newman";
}
