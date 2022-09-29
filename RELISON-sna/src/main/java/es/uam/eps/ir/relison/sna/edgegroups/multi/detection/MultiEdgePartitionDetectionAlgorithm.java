/*
 *  Copyright (C) 2022-2022 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es / Terrier Team at University of Glasgow,
 *  http://http://terrierteam.dcs.gla.ac.uk
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.sna.edgegroups.multi.detection;

import es.uam.eps.ir.relison.graph.multigraph.MultiGraph;
import es.uam.eps.ir.relison.sna.edgegroups.multi.MultiEdgePartition;

/**
 * Algorithm for detecting the edge partition of a multigraph.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface MultiEdgePartitionDetectionAlgorithm<U>
{
    /**
     * Computes the edge partition for a certain graph.
     *
     * @param graph The full graph.
     *
     * @return The edge partition if everything went OK, null if not.
     */
    MultiEdgePartition<U> detectPartition(MultiGraph<U> graph);

}
