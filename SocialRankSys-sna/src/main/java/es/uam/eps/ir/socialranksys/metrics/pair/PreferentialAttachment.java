/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.pair;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.metrics.PairMetric;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Expanded neighbor overlap. Finds the size of the intersection between
 * users at distance at most 2 of one user, with the neighborhood of another.
 * All users are given the same weight.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public class PreferentialAttachment<U> implements PairMetric<U>
{
    /**
     * Orientation selection for the origin user.
     */
    private final EdgeOrientation uSel;
    /**
     * Orientation selection for the destination user.
     */
    private final EdgeOrientation vSel;
    
    /**
     * Constructor.
     * @param uSel orientation selection for the origin user.
     * @param vSel orientation selection for the destination user.
     */
    public PreferentialAttachment(EdgeOrientation uSel, EdgeOrientation vSel)
    {
        this.uSel = uSel;
        this.vSel = vSel;
    }
    
    @Override
    public double compute(Graph<U> graph, U orig, U dest)
    {        
        double origC = graph.getNeighbourhoodSize(orig, uSel) + 0.0;
        double destC = graph.getNeighbourhoodSize(dest, vSel) + 0.0;
        return origC*destC;
    }

    @Override
    public Map<Pair<U>,Double> compute(Graph<U> graph)
    {
        Map<Pair<U>,Double> values = new HashMap<>();
        Map<U, Double> degrees = new HashMap<>();
        
        graph.getAllNodes().forEach(u -> 
        {
            double degree = graph.getNeighbourhoodSize(u, uSel);
            graph.getAllNodes().forEach(v -> 
            {
                double vdegree;
                if(degrees.containsKey(v)) vdegree=degrees.get(v);
                else
                {
                    vdegree = degrees.containsKey(v) ? degrees.get(v) : graph.getNeighbourhoodSize(v, vSel);
                    degrees.put(v, vdegree);
                }
                
                values.put(new Pair<>(u,v), degree*vdegree);
            });
        });
        
        return values;
    }

    @Override
    public Map<Pair<U>,Double> compute(Graph<U> graph, Stream<Pair<U>> pairs)
    {
        Map<Pair<U>, Double> values = new ConcurrentHashMap<>();
        Map<U, Double> origins = new ConcurrentHashMap<>();
        Map<U, Double> dests = new ConcurrentHashMap<>();
        
        pairs.forEach(pair -> 
        {
            U u = pair.v1();
            U v = pair.v2();
            double origdeg;
            double destdeg;
            
            if(origins.containsKey(u)) origdeg = origins.get(u);
            else
            {
                origdeg = graph.getNeighbourhoodSize(u, uSel) + 0.0;
                origins.put(u, origdeg);
            }

            if(dests.containsKey(v)) destdeg = dests.get(v);
            else
            {
                destdeg = graph.getNeighbourhoodSize(v, vSel) + 0.0;
                dests.put(v, destdeg);
            }
            
            double value = origdeg*destdeg;
            values.put(pair, value);
        });
        
        return values;
    }

    @Override
    public double averageValue(Graph<U> graph) 
    {
        double value = this.compute(graph).values().stream().reduce(0.0, Double::sum);
        return value/(graph.getEdgeCount()+0.0);
    }
    
    @Override 
    public double averageValue(Graph<U> graph, Stream<Pair<U>> edges, int edgeCount)
    {
        double value = edges.mapToDouble(edge -> this.compute(graph, edge.v1(), edge.v2())).sum();
        return value / (edgeCount + 0.0);
    }

    @Override
    public Function<U, Double> computeOrig(Graph<U> graph, U orig)
    {
        return this.computeIndividual(graph, orig, uSel, vSel);
    }

    @Override
    public Function<U, Double> computeDest(Graph<U> graph, U dest)
    {
        return this.computeIndividual(graph, dest, vSel, uSel);
    }

    /**
     * Computes the map of metrics for the user.
     * @param graph the graph.
     * @param u the user.
     * @param uSel the neighborhood selection for the user.
     * @param vSel the neighborhood selection for the other users
     * @return the map of metrics for the user.
     */
    private Function<U, Double> computeIndividual(Graph<U> graph, U u, EdgeOrientation uSel, EdgeOrientation vSel)
    {
        Object2DoubleMap<U> map = new Object2DoubleOpenHashMap<>();
        map.defaultReturnValue(0.0);

        double uVal = graph.getNeighbourhoodSize(u, uSel);
        if(uVal > 0)
        {
            graph.getAllNodes().forEach(v -> map.put(v, uVal * graph.getNeighbourhoodSize(v, vSel)));
        }
        return v -> map.getOrDefault(v, map.defaultReturnValue());
    }
    
}