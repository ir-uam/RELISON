/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.examples.graphgen;

/**
 * A list containing the names of graph generation algorithms.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class GraphGenerationAlgorithms
{
    /**
     * Identifier for empty graph generators.
     */
    public final static String EMPTY = "empty";
    /**
     * Identifier for complete graph generators.
     */
    public final static String COMPLETE = "complete";
    /**
     * Identifier for random Erdos graph generators.
     */
    public final static String ERDOS = "random";
    /**
     * Identifier for preferential attachment graph generators.
     */
    public final static String BARABASI = "preferential-attachment";
    /**
     * Identifier for Watts-Strogatz graph generators.
     */
    public final static String WATTS = "watts-strogatz";
}
