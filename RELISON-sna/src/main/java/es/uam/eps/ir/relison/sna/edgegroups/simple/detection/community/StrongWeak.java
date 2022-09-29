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
import es.uam.eps.ir.relison.sna.edgegroups.simple.SimpleEdgePartition;
import org.jooq.lambda.tuple.Tuple2;

/**
 * Class for dividing edges into strong / weak links (weaks inside / between communities)
 * .
 * @author Javier Sanz-Cruzado (javier.sanz-cruzadopuig@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class StrongWeak<U> extends CommunityBased<U>
{
    /**
     * Constant for referring to strong ties (links inside communities).
     */
    public final static int STRONG = 0;
    /**
     * Constant for referring to weak ties (links between communities).
     */
    public final static int WEAK = 1;
    /**
     * Constructor.
     *
     * @param comms the set of communities.
     */
    public StrongWeak(Communities<U> comms)
    {
        super(comms);
    }

    @Override
    public SimpleEdgePartition<U> detectPartition(Graph<U> graph)
    {
        SimpleEdgePartition<U> partition = new SimpleEdgePartition<>(graph.isDirected());
        partition.addGroup(STRONG);
        partition.addGroup(WEAK);

        graph.getAllNodes().forEach(u ->
        {
            int uComm = this.comms.getCommunity(u);
            graph.getAdjacentNodes(u).forEach(v ->
            {
                int vComm = this.comms.getCommunity(v);
                if(uComm == vComm)
                {
                    partition.add(new Tuple2<>(u,v), STRONG);
                }
                else
                {
                    partition.add(new Tuple2<>(u,v), WEAK);
                }
            });
        });

        return partition;
    }
}