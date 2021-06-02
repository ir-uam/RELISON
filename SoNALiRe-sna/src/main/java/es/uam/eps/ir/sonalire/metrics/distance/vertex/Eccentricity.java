/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.metrics.distance.vertex;

import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.metrics.VertexMetric;
import es.uam.eps.ir.sonalire.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.sonalire.metrics.distance.FastDistanceCalculator;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * Metric that computes the eccentricity of the nodes.
 *
 * <p>
 * <b>Reference:</b> P. Dankelmann, W. Goddard, C. Swart. The average eccentricity of a graph and its subgraphs. Utilitas Mathematica 65(May), pp. 41-51 (2004)
 * </p>
 *
 * @param <U> Type of the vertices.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Eccentricity<U> implements VertexMetric<U>
{
    /**
     * Distance calculator.
     */
    private final DistanceCalculator<U> dc;

    /**
     * Constructor.
     */
    public Eccentricity()
    {
        this.dc = new FastDistanceCalculator<>();
    }

    /**
     * Constructor.
     *
     * @param dc distance calculator.
     */
    public Eccentricity(DistanceCalculator<U> dc)
    {
        this.dc = dc;
    }

    @Override
    public double compute(Graph<U> graph, U user)
    {
        this.dc.computeDistances(graph);
        double value = 0.0;
        Map<U, Double> distances = this.dc.getDistancesFrom(user);
        for (double aux : distances.values())
        {
            if (aux > value && aux != Double.POSITIVE_INFINITY)
            {
                value = aux;
            }
        }
        return value;
    }

    @Override
    public Map<U, Double> compute(Graph<U> graph)
    {
        Map<U, Double> map = new HashMap<>();
        graph.getAllNodes().forEach(u -> map.put(u, this.compute(graph, u)));
        return map;
    }

    @Override
    public double averageValue(Graph<U> graph)
    {
        OptionalDouble optional = this.compute(graph).values().stream().mapToDouble(val -> val).average();
        return optional.isPresent() ? optional.getAsDouble() : 0.0;
    }
}
