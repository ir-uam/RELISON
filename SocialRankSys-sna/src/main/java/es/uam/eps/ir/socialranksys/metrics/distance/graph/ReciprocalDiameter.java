/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.distance.graph;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.metrics.GraphMetric;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;

/**
 * Computes the diameter of a network.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class ReciprocalDiameter<U> implements GraphMetric<U>
{
    /**
     * Distance calculator
     */
    private final Diameter<U> diameter;

    /**
     * Constructor.
     * @param dc distance calculator.
     */
    public ReciprocalDiameter(DistanceCalculator<U> dc)
    {
        this.diameter = new Diameter<>(dc);
    }

    /**
     * Constructor.
     */
    public ReciprocalDiameter()
    {
        this.diameter = new Diameter<>();
    }
    
    @Override
    public double compute(Graph<U> graph)
    {
        double value = diameter.compute(graph);
        if(value == 0) return 0.0;
        return 1.0/value;
    }
}
