/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.community.graph;

import es.uam.eps.ir.relison.community.Communities;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.multigraph.MultiGraph;

/**
 * Generates a multi-graph based on the community partition of a network.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface CommunityGraphGenerator<U>
{
    /**
     * Given a graph and its community partition, generates the community graph.
     *
     * @param graph       the original network.
     * @param communities the community partition of the network.
     *
     * @return the community-based multigraph.
     */
    MultiGraph<Integer> generate(Graph<U> graph, Communities<U> communities);
}
