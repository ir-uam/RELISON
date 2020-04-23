/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.metrics.distance.graph;

import es.uam.eps.ir.socialnetwork.metrics.GraphMetric;
import es.uam.eps.ir.socialnetwork.metrics.VertexMetric;
import es.uam.eps.ir.socialnetwork.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.socialranksys.graph.Graph;

import java.util.Map;

/**
 * Computes the diameter of a network.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class Diameter<U> implements GraphMetric<U> 
{
    /**
     * Distance calculator
     */
    private final DistanceCalculator<U> dc;
    
    /**
     * Constructor.
     * @param dc distance calculator.
     */
    public Diameter(DistanceCalculator<U> dc)
    {
        this.dc = dc;
    }
    
    /**
     * Constructor.
     */
    public Diameter()
    {
        this.dc = new DistanceCalculator<>();
    }
    
    @Override
    public double compute(Graph<U> graph)
    {
        VertexMetric<U> ecc = new Eccentricity<>(this.dc);
        Map<U, Double> map = ecc.compute(graph);
        double diameter = 0.0;
        for(double d : map.values())
        {
            if(d > diameter)
                diameter = d;
        }
        
        return diameter;
    }
}
