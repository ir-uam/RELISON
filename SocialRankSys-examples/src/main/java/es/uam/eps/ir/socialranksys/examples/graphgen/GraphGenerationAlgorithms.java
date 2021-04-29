/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.examples.graphgen;

/**
 * A list containing the names of graph generation algorithms.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class GraphGenerationAlgorithms
{
    public final static String EMPTY = "empty";
    public final static String COMPLETE = "complete";
    public final static String ERDOS = "erdos";
    public final static String BARABASI = "barabasi";
    public final static String WATTS = "watts-strogatz";
}
