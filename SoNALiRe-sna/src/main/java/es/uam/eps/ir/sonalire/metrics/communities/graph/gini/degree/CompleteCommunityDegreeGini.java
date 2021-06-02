/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.metrics.communities.graph.gini.degree;

import es.uam.eps.ir.sonalire.community.graph.CompleteCommunityGraphGenerator;
import es.uam.eps.ir.sonalire.community.graph.CompleteCommunityNoSelfLoopsGraphGenerator;
import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;

/**
 * Computes the community degree Gini of the graph, i.e. the Gini coefficient for the
 * degree distribution of the communities in the graph. This version considers both inter-community
 * and intra-community links.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class CompleteCommunityDegreeGini<U> extends CommunityDegreeGini<U>
{
    /**
     * Constructor
     *
     * @param orientation Orientation of the edges.
     * @param autoloops   Indicates if autoloops are allowed
     */
    public CompleteCommunityDegreeGini(EdgeOrientation orientation, boolean autoloops)
    {
        super(orientation, autoloops ? new CompleteCommunityGraphGenerator<>() : new CompleteCommunityNoSelfLoopsGraphGenerator<>());
    }

}
