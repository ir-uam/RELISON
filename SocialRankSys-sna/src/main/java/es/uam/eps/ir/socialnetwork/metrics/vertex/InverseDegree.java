/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.metrics.vertex;

import es.uam.eps.ir.socialnetwork.metrics.VertexMetric;
import es.uam.eps.ir.socialranksys.graph.DirectedGraph;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;

/**
 * Computes the inverse of the degree of a node.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public class InverseDegree<U> implements VertexMetric<U>
{
    /**
     * The orientation in which the score is computed..
     */
    EdgeOrientation orientation;
    
    /**
     * Constructor.
     * @param orientation The orientation in which the score is computed.
     */
    public InverseDegree(EdgeOrientation orientation)
    {
        this.orientation = orientation;
    }
    
    @Override
    public double compute(Graph<U> graph, U user)
    {

        if(!graph.isDirected() || orientation.equals(EdgeOrientation.UND))
        {
            return 1.0/(graph.degree(user)+1.0);
        }
        else if(orientation.equals(EdgeOrientation.IN))
        {
            DirectedGraph<U> g = (DirectedGraph<U>) graph;
            return 1.0/(g.inDegree(user)+1.0);
        }
        else
        {
            DirectedGraph<U> g = (DirectedGraph<U>) graph;
            return 1.0/(g.outDegree(user)+1.0);
        }
    }
}
