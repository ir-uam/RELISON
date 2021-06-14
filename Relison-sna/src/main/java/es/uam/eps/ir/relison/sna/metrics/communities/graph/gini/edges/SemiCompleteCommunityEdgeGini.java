/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.sna.metrics.communities.graph.gini.edges;

import es.uam.eps.ir.relison.sna.metrics.graph.EdgeGiniMode;

/**
 * Computes the community edge Gini of the graph, i.e. the Gini coefficient for the
 * number of edges between each pair of communities. This version considers all pairs
 * of nodes in the calculus (i.e. it includes pairings of a community with itself), but
 * autoloops are considered as a whole set of links, instead of separately for each community.
 * <p>
 * <b>References: </b></p>
 *     <ol>
 *         <li>J. Sanz-Cruzado, P. Castells. Enhancing structural diversity in social networks by recommending weak ties. 12th ACM Conference on Recommender Systems (RecSys 2018),pp. 233-241 (2018) </li>
 *     </ol>
 *
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class SemiCompleteCommunityEdgeGini<U> extends CommunityEdgeGini<U>
{
    /**
     * Constructor.
     *
     * @param selfloops true if selfloops are allowed, false if they are not.
     */
    public SemiCompleteCommunityEdgeGini(boolean selfloops)
    {
        super(EdgeGiniMode.SEMICOMPLETE, selfloops);
    }
}
