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

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.multigraph.MultiGraph;
import es.uam.eps.ir.relison.sna.edgegroups.multi.MultiEdgePartition;
import es.uam.eps.ir.relison.sna.edgegroups.simple.SimpleEdgePartition;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple3;

import java.util.List;

/**
 * Class for constructing edge partition from edge types.
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzadopuig@glasgow.ac.uk)
 */
public class TypeBased<U> implements MultiEdgePartitionDetectionAlgorithm<U>
{
    @Override
    public MultiEdgePartition<U> detectPartition(MultiGraph<U> graph)
    {
        MultiEdgePartition<U> partition = new MultiEdgePartition<>(graph.isDirected());
        graph.getAllNodes().forEach(u ->
            graph.getAdjacentNodesTypesLists(u).forEach(wV ->
            {
                U v = wV.getIdx();
                int numEdges = wV.getValue().size();
                List<Integer> types = wV.getValue();
                for(int i = 0; i < numEdges; ++i)
                {
                    int type = types.get(i);
                    if (!partition.hasGroup(type))
                        partition.addGroup(type);
                    partition.add(new Tuple3<>(u, v, i), type);
                }
            })
        );

        return partition;
    }
}
