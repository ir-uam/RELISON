/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.metrics.graph;

/**
 * Different execution modes for the PairGini metric.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public enum EdgeGiniMode
{
    /**
     * Gini is computed over all pairs of users.
     */
    COMPLETE,
    /**
     * Gini is computed over all pairs of users, but self-loops are considered in a separate category (all together)
     */
    SEMICOMPLETE,
    /**
     * Self-loops are ignored.
     */
    INTERLINKS
}
