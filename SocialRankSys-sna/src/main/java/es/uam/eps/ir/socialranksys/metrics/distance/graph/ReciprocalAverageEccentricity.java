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
import es.uam.eps.ir.socialranksys.metrics.distance.vertex.Eccentricity;

import java.util.Map;

/**
 * Computes the diameter of a network.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class ReciprocalAverageEccentricity<U> implements GraphMetric<U>
{
    /**
     * Distance calculator
     */
    private final Eccentricity<U> eccentricity;

    /**
     * Constructor.
     * @param dc distance calculator.
     */
    public ReciprocalAverageEccentricity(DistanceCalculator<U> dc)
    {
        this.eccentricity = new Eccentricity<>(dc);
    }

    /**
     * Constructor.
     */
    public ReciprocalAverageEccentricity()
    {
        this.eccentricity = new Eccentricity<>();
    }
    
    @Override
    public double compute(Graph<U> graph)
    {
        Map<U, Double> map = this.eccentricity.compute(graph);
        double sum = map.values().stream().mapToDouble(x -> x).sum();

        if(sum == 0) return 0.0;
        else return (graph.getVertexCount()+0.0)/sum;
    }
}