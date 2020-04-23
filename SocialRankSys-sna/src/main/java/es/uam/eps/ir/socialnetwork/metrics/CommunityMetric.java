/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.metrics;


import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.graph.Graph;

/**
 * Global metric that depends on the communities of the graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users.
 */
public interface CommunityMetric<U> 
{
    /**
     * Computes the value of the metric in the graph.
     * @param graph Graph.
     * @param comm Communities of the graph.
     * @return the value of the metric.
     */
    double compute(Graph<U> graph, Communities<U> comm);
}
