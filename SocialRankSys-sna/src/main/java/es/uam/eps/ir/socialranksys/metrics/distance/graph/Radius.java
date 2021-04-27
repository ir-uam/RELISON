/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.distance.graph;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.metrics.GraphMetric;
import es.uam.eps.ir.socialranksys.metrics.VertexMetric;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.socialranksys.metrics.distance.FastDistanceCalculator;
import es.uam.eps.ir.socialranksys.metrics.distance.vertex.Eccentricity;

import java.util.Map;

/**
 * Computes the radius of a network. The radius is the minimum
 * eccentricity of the network.
 *
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Radius<U> implements GraphMetric<U>
{
    /**
     * Distance calculator.
     */
    private final DistanceCalculator<U> dc;

    /**
     * Constructor.
     *
     * @param dc distance calculator.
     */
    public Radius(DistanceCalculator<U> dc)
    {
        this.dc = dc;
    }

    /**
     * Constructor.
     */
    public Radius()
    {
        this.dc = new FastDistanceCalculator<>();
    }

    @Override
    public double compute(Graph<U> graph)
    {
        VertexMetric<U> ecc = new Eccentricity<>(this.dc);
        Map<U, Double> map = ecc.compute(graph);
        double radius = Double.POSITIVE_INFINITY;
        for (double d : map.values())
        {
            if (d < radius)
            {
                radius = d;
            }
        }

        return radius;
    }
}
