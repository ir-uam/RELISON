/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.metrics.communities.graph.gini.edges;

import es.uam.eps.ir.socialnetwork.metrics.graph.EdgeGiniMode;

/**
 * Computes the community edge Gini of the graph, i.e. the Gini coefficient for the
 * number of edges between each pair of communities. This version considers all pairs 
 * of nodes in the calculus (i.e. it includes pairings of a community with itself).
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class CompleteCommunityEdgeGini<U> extends CommunityEdgeGini<U>
{
    /**
     * Constructor.
     * @param autoloops true if autoloops are allowed, false if they are not.
     */
    public CompleteCommunityEdgeGini(boolean autoloops)
    {
        super(EdgeGiniMode.COMPLETE, autoloops);
    }
}
