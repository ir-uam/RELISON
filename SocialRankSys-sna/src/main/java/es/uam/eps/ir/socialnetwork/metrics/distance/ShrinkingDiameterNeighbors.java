/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.metrics.distance;

import es.uam.eps.ir.socialnetwork.metrics.pair.AbstractPairMetric;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Computes the variation of the average shortest path length between the neighbors
 * of a pair of nodes if the link was added to the graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users.
 */
public class ShrinkingDiameterNeighbors<U> extends AbstractPairMetric<U>
{
    /**
     * Distance calculator.
     */
    private final DistanceCalculator<U> dc;
    /**
     * Neighborhood selection for the origin user.
     */
    private final EdgeOrientation uSel;
    /**
     * Neighborhood selection for the destination user.
     */
    private final EdgeOrientation vSel;
    
    /**
     * Constructor. 
     * @param uSel neighborhood selection for the origin user.
     * @param vSel neighborhood selection for the destination user.
     */
    public ShrinkingDiameterNeighbors(EdgeOrientation uSel, EdgeOrientation vSel)
    {
        dc = new DistanceCalculator<>();
        this.uSel = uSel;
        this.vSel = vSel;

    }
    
    /**
     * Constructor. 
     * @param dc distance calculator.
     * @param uSel neighborhood selection for the origin user.
     * @param vSel neighborhood selection for the destination user.
     */
    public ShrinkingDiameterNeighbors(DistanceCalculator<U> dc, EdgeOrientation uSel, EdgeOrientation vSel)
    {
        this.dc = dc;
        this.uSel = uSel;
        this.vSel = vSel;

    }
        
    @Override
    public double compute(Graph<U> graph, U orig, U dest)
    {
        if(graph.containsEdge(orig, dest)) return 0.0;
        // First, we find the pairs of users at maximum distance (diameter)
        this.dc.computeDistances(graph);
        
        Map<U, Map<U, Double>> distances = this.dc.getDistances();
        
        Set<U> uNeighs = graph.getNeighbourhood(orig, uSel).collect(Collectors.toCollection(HashSet::new));
        Set<U> vNeighs = graph.getNeighbourhood(dest, vSel).collect(Collectors.toCollection(HashSet::new));
        
        Set<U> union = uNeighs;
        union.addAll(vNeighs);
        union.add(orig);
        union.add(dest);
        
        return this.compute(orig, dest, union, distances);
    }
    
    private double compute(U orig, U dest, Set<U> union, Map<U,Map<U,Double>> distances)
    {      
        double currentDiam = 0.0;
        double newDiam = 0.0;
        
        for(U w1 : union)
        {
            for(U w2 : union)
            {
                double dist = distances.get(w1).get(w2);
                double aux = distances.get(w1).get(orig) + 1.0 + distances.get(dest).get(w2);
                if(Double.isFinite(dist))
                {
                    if(dist > currentDiam) currentDiam = dist;
                    double min = Math.min(dist, aux);
                    if(min > newDiam) newDiam = dist;
                }
                else
                {
                    if(Double.isFinite(aux))
                    {
                        if(aux > newDiam) newDiam = aux;
                    }
                }
                
            }
        }
        
        
        return currentDiam - newDiam;
    }

    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph) 
    {
        // First, we find the pairs of users at maximum distance (diameter)
        this.dc.computeDistances(graph);
        
        Map<U, Map<U, Double>> distances = this.dc.getDistances();
       
        Map<Pair<U>, Double> values = new HashMap<>();
        
        // Then, for each pair of users in the network, find the value of the metric
        graph.getAllNodes().forEach(u -> 
        {
            Set<U> uCIn = graph.getNeighbourhood(u, uSel).collect(Collectors.toCollection(HashSet::new));
            
            graph.getAllNodes().forEach(v -> 
            {
                Set<U> vCIn = graph.getNeighbourhood(v, vSel).collect(Collectors.toCollection(HashSet::new));
                Set<U> union = new HashSet<>(uCIn);
                union.add(u);
                union.add(v);
                union.addAll(vCIn);
                
                double value = this.compute(u,v,union,distances);
                values.put(new Pair<>(u,v), value);
            });
        });
        
        return values;
    }
    
    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph, Stream<Pair<U>> pairs) 
    {
        // First, we find the pairs of users at maximum distance (diameter)
        this.dc.computeDistances(graph);
        
        Map<U, Map<U, Double>> distances = this.dc.getDistances();
       
        Map<Pair<U>, Double> values = new HashMap<>();
        
        // Then, for each pair of users in the network, find the value of the metric
        pairs.forEach(pair -> 
        {
            U u = pair.v1();
            U v = pair.v2();
            Set<U> uCIn = graph.getNeighbourhood(u, uSel).collect(Collectors.toCollection(HashSet::new));
            Set<U> vCIn = graph.getNeighbourhood(v, vSel).collect(Collectors.toCollection(HashSet::new));
            Set<U> union = new HashSet<>(uCIn);
            union.add(u);
            union.add(v);
            union.addAll(vCIn);

            double value = this.compute(u,v,union,distances);
            values.put(new Pair<>(u,v), value);
        });
        
        return values;
    }
    
    @Override
    public double averageValue(Graph<U> graph) 
    {
        Map<Pair<U>, Double> values = this.compute(graph);
        OptionalDouble opt = values.values().stream().mapToDouble(x -> x).average();
        return opt.isPresent() ? opt.getAsDouble() : Double.NaN;
    }

    @Override
    public double averageValue(Graph<U> graph, Stream<Pair<U>> pair, int pairCount) 
    {
        Map<Pair<U>, Double> values = this.compute(graph, pair);
        OptionalDouble opt = values.values().stream().mapToDouble(x->x).average();
        return opt.isPresent() ? opt.getAsDouble() : Double.NaN;
    }    
}
