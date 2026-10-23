/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.sna.edge.detection;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.sna.edge.EdgePartition;

/**
 * Algorithm for partitioning the edges of a graph into disjoint groups.
 *
 * @param <U> type of the nodes.
 */
public interface EdgePartitionDetector<U>
{
    /**
     * Computes an edge partition for a graph.
     *
     * @param graph the full graph.
     * @return the edge partition if successful, or {@code null} if it could not be computed.
     */
    EdgePartition<U> detectEdgePartition(Graph<U> graph);
}
