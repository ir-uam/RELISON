/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.communities.graph.gini.degree;


import es.uam.eps.ir.socialranksys.community.graph.InterCommunityGraphGenerator;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;

/**
 * Computes the community degree Gini of the graph, i.e. the Gini coefficient for the
 * degree distribution of the communities in the graph. This version only considers
 * the inter-community links for the calculus.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public class InterCommunityDegreeGini<U> extends CommunityDegreeGini<U> 
{
    /**
     * Constructor
     * @param orientation Orientation of the edges. 
     */
    public InterCommunityDegreeGini(EdgeOrientation orientation)
    {
        super(orientation, new InterCommunityGraphGenerator<>());
    }
    
}
