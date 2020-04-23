/* 
 * Copyright (C) 2018 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.community.graph;

import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.multigraph.MultiGraph;

/**
 * Generates a multi-graph based on the community partition of a network.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public interface CommunityGraphGenerator<U> 
{
    /**
     * Given a graph and its community partition, generates the community graph.
     * @param graph the original network.
     * @param communities the community partition of the network.
     * @return the community-based multigraph.
     */
    MultiGraph<Integer> generate(Graph<U> graph, Communities<U> communities);
}
