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
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.metrics.VertexMetric;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 *
 * @author Javier
 * @param <U>
 */
public class FreeDiscovery<U> implements VertexMetric<U>
{
    /**
     * Orientation.
     */    
    private final EdgeOrientation orient;
    
    /**
     * Constructor.
     * @param orient edge orientation. 
     */
    public FreeDiscovery(EdgeOrientation orient)
    {
        this.orient = orient;
    }
    
    @Override
    public Map<U, Double> compute(Graph<U> graph)
    {
        Map<U, Double> values = new HashMap<>();
        Map<U, Double> sizes = new HashMap<>();
        double value = graph.getAllNodes().mapToDouble(u ->
        {
            double val = graph.getNeighbourhoodSize(u, orient);
            sizes.put(u, val);
            return val;
        }).sum();
        graph.getAllNodes().forEach(u -> values.put(u, sizes.get(u)/value));
        
        return values;
    }
    
    @Override
    public Map<U, Double> compute(Graph<U> graph, Stream<U> users)
    {
        Map<U, Double> values = new ConcurrentHashMap<>();
        Map<U, Double> sizes = new ConcurrentHashMap<>();
        double value = graph.getAllNodes().mapToDouble(u ->
        {
            double val = graph.getNeighbourhoodSize(u, orient);
            sizes.put(u, val);
            return val;
        }).sum();
        users.filter(graph::containsVertex).forEach(u -> values.put(u, sizes.get(u)/value));
        return values;
    }
    
    @Override
    public double compute(Graph<U> graph, U user) 
    {
        double value = graph.getAllNodes().mapToDouble(u -> graph.getNeighbourhoodSize(u, orient)).sum();
        return graph.getNeighbourhoodSize(user, orient)/value;
    }
    
}
