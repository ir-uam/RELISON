/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.metrics.distance.modes;

/**
 * Algorithms for computing the closeness of a node in a graph.
 * <ul>
 *  <li><b>HARMONICMEAN:</b> Computes the closeness as the harmonic mean of the distances from the node to the rest.</li>
 *  <li><b>COMPONENTS:</b> Computes the closeness inside each separate strongly connected component. In case the graph is strongly connected, it represents the classical definition</li>
 * </ul>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public enum ClosenessMode
{
    /**
     * Closeness is computed as the harmonic mean of the distances.
     */
    HARMONICMEAN,
    /**
     * Closeness is computed inside each strongly connected component.
     */
    COMPONENTS
}
