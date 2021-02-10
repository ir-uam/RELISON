/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.communities.graph.gini.edges.dice;

import es.uam.eps.ir.socialranksys.metrics.graph.EdgeGiniMode;

/**
 * Computes the Dice community edge Gini of the graph, i.e. the Gini coefficient over the
 * pairs of communities in the graph, where the frequency is the relation between the real
 * number of edges between communities, and the number of edges between communities in a
 * configuration network where the link degree distributions are equivalent to the ones in the
 * studied network. This version considers all pairs of different communities in the calculus
 * (i.e. it does not include pairings of a community with itself).
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class DiceInterCommunityEdgeGini<U> extends DiceCommunityEdgeGini<U>
{
    /**
     * Constructor.
     */
    public DiceInterCommunityEdgeGini()
    {
        super(EdgeGiniMode.INTERLINKS, false);
    }
}
