/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.vertex;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.Weight;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.metrics.VertexMetric;

/**
 * Class that measures the length (sum of weights of a selection of the edges concerning it) of a vertex.
 * @author Javier Sanz-Cruzado Puig
 */
public class VertexLength<U> implements VertexMetric<U>
{

    private final EdgeOrientation uSel;

    public VertexLength(EdgeOrientation uSel)
    {
        this.uSel = uSel;
    }
    
    @Override
    public double compute(Graph<U> graph, U user)
    {
        double average = graph.isWeighted() ? graph.getAllNodes().mapToDouble(x -> graph.getAdjacentNodesWeights(x)
                                                                                        .mapToDouble(Weight::getValue)
                                                                                        .sum())
                                                                 .sum()
                                            : (graph.getEdgeCount() + 0.0);
        
        double val = graph.isWeighted() ? graph.getNeighbourhoodWeights(user, uSel).mapToDouble(Weight::getValue).sum() :
                graph.getNeighbourhoodSize(user, uSel);
        
        return 1.0/(1.0 + Math.log(1 + average/(val+1.0))/Math.log(2.0));
    }
    
}
