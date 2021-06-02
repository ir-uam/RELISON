/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.metrics;

import es.uam.eps.ir.sonalire.graph.Graph;

/**
 * Interface for global graph metrics.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface GraphMetric<U>
{
    /**
     * Computes the value.
     *
     * @param graph graph metric.
     *
     * @return the value of the metric.
     */
    double compute(Graph<U> graph);
}
