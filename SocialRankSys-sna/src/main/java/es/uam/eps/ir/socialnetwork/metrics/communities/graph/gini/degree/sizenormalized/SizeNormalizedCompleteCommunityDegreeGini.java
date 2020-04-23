/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.metrics.communities.graph.gini.degree.sizenormalized;


import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.community.graph.CompleteCommunityGraphGenerator;
import es.uam.eps.ir.socialranksys.community.graph.CompleteCommunityNoAutoloopsGraphGenerator;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;

/**
 * Computes the community degree Gini of the graph, i.e. the Gini coefficient for the
 * degree distribution of the communities in the graph. This version considers both inter-community
 * and intra-community links.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public class SizeNormalizedCompleteCommunityDegreeGini<U> extends SizeNormalizedCommunityDegreeGini<U> 
{
    /**
     * Constructor.
     * @param orientation Orientation of the edges. 
     * @param autoloops indicates if autoloops between nodes are allowed or not.
     */
    public SizeNormalizedCompleteCommunityDegreeGini(EdgeOrientation orientation, boolean autoloops)
    {
        super(orientation, autoloops ? new CompleteCommunityGraphGenerator<>() : new CompleteCommunityNoAutoloopsGraphGenerator<>());
    }

    @Override
    protected double getDenom(Graph<U> graph, Communities<U> comm, int c)
    {
        if(graph.isDirected() && this.getOrientation().equals(EdgeOrientation.UND))
        {
            return 2.0*comm.getCommunitySize(c);
        }
        else
        {
            return comm.getCommunitySize(c);
        }
    }
    
}
