/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.metrics.distance.graph;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.metrics.GraphMetric;
import es.uam.eps.ir.relison.metrics.VertexMetric;
import es.uam.eps.ir.relison.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.relison.metrics.distance.FastDistanceCalculator;
import es.uam.eps.ir.relison.metrics.distance.vertex.Eccentricity;

import java.util.Map;

/**
 * Computes the diameter of a network.
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Diameter<U> implements GraphMetric<U>
{
    /**
     * Distance calculator
     */
    private final DistanceCalculator<U> dc;

    /**
     * Constructor.
     *
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
        this.dc = new FastDistanceCalculator<>();
    }

    @Override
    public double compute(Graph<U> graph)
    {
        VertexMetric<U> ecc = new Eccentricity<>(this.dc);
        Map<U, Double> map = ecc.compute(graph);
        double diameter = 0.0;
        for (double d : map.values())
        {
            if (d > diameter)
            {
                diameter = d;
            }
        }

        return diameter;
    }
}
