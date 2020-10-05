/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.communities.graph.gini.edges.sizenormalized;

import es.uam.eps.ir.socialranksys.metrics.graph.EdgeGiniMode;

/**
 * Computes the size normalized community edge Gini of the graph, i.e. the Gini coefficient
 * for the number of edges in the graph, divided by the maximum number of possible links between
 * the endpoint communities. This version considers all pairs
 * of different communities in the calculus (i.e. it does not include pairings of a community with itself).
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class SizeNormalizedInterCommunityEdgeGini<U> extends SizeNormalizedCommunityEdgeGini<U>
{
    /**
     * Constructor.
     */
    public SizeNormalizedInterCommunityEdgeGini()
    {
        super(EdgeGiniMode.INTERLINKS, false);
    }
}
