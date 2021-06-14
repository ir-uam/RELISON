/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.sna.metrics.communities.graph.gini.degree;


import es.uam.eps.ir.relison.sna.community.graph.InterCommunityGraphGenerator;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;

/**
 * Computes the community degree Gini of the graph, i.e. the Gini coefficient for the
 * degree distribution of the communities in the graph. This version only considers
 * the inter-community links for the calculus.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class InterCommunityDegreeGini<U> extends CommunityDegreeGini<U>
{
    /**
     * Constructor.
     *
     * @param orientation Orientation of the edges.
     */
    public InterCommunityDegreeGini(EdgeOrientation orientation)
    {
        super(orientation, new InterCommunityGraphGenerator<>());
    }

}
