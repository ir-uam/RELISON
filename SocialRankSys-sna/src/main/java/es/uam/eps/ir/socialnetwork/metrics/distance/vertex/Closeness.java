/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.metrics.distance.vertex;

import es.uam.eps.ir.socialnetwork.metrics.VertexMetric;
import es.uam.eps.ir.socialnetwork.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.socialnetwork.metrics.distance.modes.ClosenessMode;
import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.graph.Graph;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * Metric that computes the closeness of the nodes.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 * 
 * Centrality in Networks: I. Conceptual clarification. Freeman, Linton C., Social Networks 1, 1979, pp.215-239
 * 
 */
public class Closeness<U> implements VertexMetric<U>
{
    /**
     * Distance calculator.
     */
    private final DistanceCalculator<U> dc;
    /**
     * Calculation mode
     */
    private final ClosenessMode mode;
    
    /**
     * Basic constructor. Uses the harmonic mean computing algorithm.
     */
    public Closeness()
    {
        this(new DistanceCalculator<>(), ClosenessMode.HARMONICMEAN);
    }
    
    /**
     * Constructor.
     * @param mode Computing algorithm to use.
     */
    public Closeness(ClosenessMode mode)
    {
        this(new DistanceCalculator<>(),  mode);
    }
    
    
    /**
     * Constructor. Uses the harmonic mean computing algorithm.
     * @param dc distance calculator.
     */
    public Closeness(DistanceCalculator<U> dc)
    {
        this(dc, ClosenessMode.HARMONICMEAN);
    }

    /**
     * Constructor.
     * @param dc distance calculator.
     * @param mode computing algorithm.
     */
    public Closeness(DistanceCalculator<U> dc, ClosenessMode mode)
    {
        this.dc = dc;
        this.mode = mode;
    }
    
    @Override
    public double compute(Graph<U> graph, U user) {
        this.dc.computeDistances(graph);
        double value = 0.0;
        switch(this.mode)
        {
            case HARMONICMEAN:
                value = this.dc.getDistancesFrom(user).values().stream().mapToDouble(dist -> (dist.isInfinite() || dist == 0) ? 0.0 : 1.0/dist).sum();
                value /= (graph.getVertexCount()-1.0);
            break;
            case COMPONENTS:
                Communities<U> scc = this.dc.getSCC();
                int comm = scc.getCommunity(user);
                long numComm = scc.getUsers(comm).count();
                if(numComm > 1.0)
                    value = (numComm - 1.0) / scc.getUsers(comm).mapToDouble(v -> this.dc.getDistances(user, v)).sum();
            break;
        }
        return value;
    }

    @Override
    public Map<U, Double> compute(Graph<U> graph) 
    {
        Map<U, Double> closeness = new HashMap<>();
        graph.getAllNodes().forEach(u -> closeness.put(u,this.compute(graph,u)));
        return closeness;
    }

    @Override
    public double averageValue(Graph<U> graph) 
    {
        Map<U, Double> map = this.compute(graph);
        OptionalDouble optional = map.values().stream().mapToDouble(val -> val).average();
        return optional.isPresent() ? optional.getAsDouble() : 0.0;
    }
}
