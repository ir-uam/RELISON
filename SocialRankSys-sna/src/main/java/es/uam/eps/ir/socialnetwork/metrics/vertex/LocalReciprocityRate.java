/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
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
 * Computes the local reciprocity rate.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class LocalReciprocityRate<U> implements VertexMetric<U>
{
    /**
     * Orientation of the edges.
     */
    private final EdgeOrientation orient;
    
    /**
     * Constructor.
     * @param orient Orientation of the edges
     */
    public LocalReciprocityRate(EdgeOrientation orient)
    {
        this.orient = orient;
    }
    
    @Override
    public double compute(Graph<U> graph, U user)
    {
        if(!graph.isDirected())
        {
            return 1.0;
        }
        else
        {
            DirectedGraph<U> dgraph = (DirectedGraph<U>) graph;
            double num = 0.0;
            double denom = 0.0;
            
            if(orient.equals(EdgeOrientation.IN) || orient.equals(EdgeOrientation.UND))
            {
                denom += dgraph.inDegree(user);
                num += dgraph.getIncidentNodes(user).filter(w -> dgraph.containsEdge(user, w)).count() + 0.0;
            }
            
            if(orient.equals(EdgeOrientation.OUT) || orient.equals(EdgeOrientation.UND))
            {
                denom += dgraph.outDegree(user);
                num += dgraph.getAdjacentNodes(user).filter(w -> dgraph.containsEdge(w, user)).count() + 0.0;
            }
            
            return num/denom;
        }
            
    }    
}
