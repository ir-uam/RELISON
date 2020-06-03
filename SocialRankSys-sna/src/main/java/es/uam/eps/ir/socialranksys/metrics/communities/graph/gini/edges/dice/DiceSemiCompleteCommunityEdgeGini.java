/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
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
 * studied network. This version considers all pairs  * of nodes in the calculus 
 * (i.e. it includes pairings of a community with itself), but autoloops are considered as a 
 * whole set of links, instead of separately for each community.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class DiceSemiCompleteCommunityEdgeGini<U> extends DiceCommunityEdgeGini<U>
{
    /**
     * Constructor.
     * @param autoloops true if autoloops are allowed, false if not.
     */
    public DiceSemiCompleteCommunityEdgeGini(boolean autoloops)
    {
        super(EdgeGiniMode.SEMICOMPLETE, autoloops);
    }
}
