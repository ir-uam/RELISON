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
import es.uam.eps.ir.sonalire.metrics.distance.CompleteDistanceCalculator;
import es.uam.eps.ir.sonalire.metrics.distance.DistanceCalculator;

import java.util.Map;
import java.util.OptionalDouble;

/**
 * Computes the betweenness of the nodes of a graph.
 * <p>
 * <b>Reference:</b> M.E.J. Newman, M. Girvan. Finding and evaluating community structure in networks. Physical Review E 69(2), pp. 1-16 (2004)
 * </p>
 *
 * @param <U> Type of the users.
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
     * Indicates whether we have to normalize the value of the metric or not.
     */
    private final boolean normalize;

    /**
     * Constructor.
     *
     * @param normalize true if we have to normalize the value of the metric, false otherwise.
     */
    public NodeBetweenness(boolean normalize)
    {
        this.dc = new CompleteDistanceCalculator<>();
        this.normalize = normalize;
    }

    /**
     * Constructor.
     *
     * @param dc Distance calculator.
     * @param normalize true if we have to normalize the value of the metric, false otherwise.
     */
    public NodeBetweenness(DistanceCalculator<U> dc, boolean normalize)
    {
        this.dc = dc;
        this.normalize = normalize;
    }

    @Override
    public double compute(Graph<U> graph, U user)
    {
        this.dc.computeDistances(graph);
        double value = this.dc.getNodeBetweenness(user);

        if(normalize) value *= (graph.isDirected() ? 1.0 : 2.0)/((graph.getVertexCount()-2.0)*(graph.getVertexCount()-1.0));
        return value;
    }

    @Override
    public Map<U, Double> compute(Graph<U> graph)
    {
        this.dc.computeDistances(graph);
        Map<U, Double> values = this.dc.getNodeBetweenness();
        double norm = (graph.isDirected() ? 1.0 : 2.0)/((graph.getVertexCount()-2.0)*(graph.getVertexCount()-1.0));

        if(normalize) values.forEach((key, value) -> values.put(key, value*norm));
        return values;
    }

    @Override
    public double averageValue(Graph<U> graph)
    {
        OptionalDouble optional = this.compute(graph).values().stream().mapToDouble(val -> val).average();
        return optional.isPresent() ? optional.getAsDouble() : 0.0;
    }

}
