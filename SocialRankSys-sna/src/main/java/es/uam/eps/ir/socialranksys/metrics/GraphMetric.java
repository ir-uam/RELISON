/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics;

import es.uam.eps.ir.socialranksys.graph.Graph;

/**
 * Interface for global graph metrics.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public interface GraphMetric<U> 
{
    /**
     * Computes the value.
     * @param graph Graph metric.
     * @return the value of the metric.
     */
    double compute(Graph<U> graph);
}
