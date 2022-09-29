/*
 *  Copyright (C) 2022-2022 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es / Terrier Team at University of Glasgow,
 *  http://http://terrierteam.dcs.gla.ac.uk
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package es.uam.eps.ir.relison.sna.edgegroups.simple.detection.community;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.sna.edgegroups.simple.CommunityEdgePartition;
import es.uam.eps.ir.relison.sna.edgegroups.simple.InterCommunityEdgePartition;
import es.uam.eps.ir.relison.sna.edgegroups.simple.SimpleEdgePartition;
import org.jooq.lambda.tuple.Tuple2;

/**
 * Class for constructing edge partition from communities. In this case, we have a partition for every
 * pair of different communities, and another one for all links inside communities.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzadopuig@glasgow.ac.uk)
 */
public class InterCommunityLinks<U> extends CommunityBased<U>
{
    /**
     * Constructor.
     * @param communities the community partition to consider.
     */
    public InterCommunityLinks(Communities<U> communities)
    {
        super(communities);
    }

    @Override
    public SimpleEdgePartition<U> detectPartition(Graph<U> graph)
    {
        InterCommunityEdgePartition<U> partition = new InterCommunityEdgePartition<>(graph.isDirected(), this.comms.getNumCommunities());

        graph.getAllNodes().forEach(u ->
        {
            int commU = this.comms.getCommunity(u);
            graph.getAdjacentNodes(u).forEach(v ->
            {
                int slot = partition.getSlot(commU, this.comms.getCommunity(v));
                partition.add(new Tuple2<>(u,v), slot);
            });
        });

        return partition;
    }
}
