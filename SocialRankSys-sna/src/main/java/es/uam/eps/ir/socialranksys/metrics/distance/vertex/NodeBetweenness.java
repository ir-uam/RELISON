/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.distance.vertex;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.metrics.VertexMetric;
import es.uam.eps.ir.socialranksys.metrics.distance.CompleteDistanceCalculator;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;

import java.util.Map;
import java.util.OptionalDouble;

/**
 * Computes the betweenness of the nodes of a graph
 * <p>
 * <b>Reference:</b> M.E.J. Newman, M. Girvan. Finding and evaluating community structure in networks. Physical Review E 69(2), pp. 1-16 (2004)
 * </p>
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class NodeBetweenness<U> implements VertexMetric<U>
{

    /**
     * Distance calculator.
     */
    private final DistanceCalculator<U> dc;

    /**
     * Constructor
     */
    public NodeBetweenness()
    {
        this.dc = new CompleteDistanceCalculator<>();
    }

    /**
     * Constructor.
     *
     * @param dc Distance calculator.
     */
    public NodeBetweenness(DistanceCalculator<U> dc)
    {
        this.dc = dc;
    }

    @Override
    public double compute(Graph<U> graph, U user)
    {
        this.dc.computeDistances(graph);
        return this.dc.getNodeBetweenness(user);
    }

    @Override
    public Map<U, Double> compute(Graph<U> graph)
    {
        this.dc.computeDistances(graph);
        return this.dc.getNodeBetweenness();
    }

    @Override
    public double averageValue(Graph<U> graph)
    {
        OptionalDouble optional = this.compute(graph).values().stream().mapToDouble(val -> val).average();
        return optional.isPresent() ? optional.getAsDouble() : 0.0;
    }

}
