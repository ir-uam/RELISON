/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.communities.graph.gini.degree.sizenormalized;


import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.community.graph.InterCommunityGraphGenerator;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;

/**
 * Computes the community degree Gini of the graph, i.e. the Gini coefficient for the
 * degree distribution of the communities in the graph. This version only considers
 * the inter-community links for the calculus.
 *
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class SizeNormalizedInterCommunityDegreeGini<U> extends SizeNormalizedCommunityDegreeGini<U>
{
    /**
     * Constructor
     *
     * @param orientation Orientation of the edges.
     */
    public SizeNormalizedInterCommunityDegreeGini(EdgeOrientation orientation)
    {
        super(orientation, new InterCommunityGraphGenerator<>());
    }

    @Override
    protected double getDenom(Graph<U> graph, Communities<U> comm, int c)
    {
        int commSize = comm.getCommunitySize(c);

        if (graph.isDirected() && this.getOrientation().equals(EdgeOrientation.UND))
        {
            return 2.0 * commSize * (graph.getVertexCount() - commSize);
        }
        else
        {
            return commSize * (graph.getVertexCount() - commSize);
        }
    }

}
