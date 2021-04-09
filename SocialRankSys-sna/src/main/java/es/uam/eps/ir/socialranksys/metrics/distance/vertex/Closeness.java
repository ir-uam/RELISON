/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.distance.vertex;

import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.metrics.VertexMetric;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.socialranksys.metrics.distance.FastDistanceCalculator;
import es.uam.eps.ir.socialranksys.metrics.distance.modes.ClosenessMode;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * Metric that computes the closeness of the nodes.
 *
 * <p>
 * <b>References:</b><br/>
 * M.E.J. Newman. Networks: an introduction (2010)<br />
 * L.C. Freeman. Centrality in Networks: I. Conceptual clarification, Social Networks 1, 1979, pp.215-239
 * </p>
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Closeness<U> implements VertexMetric<U>
{
    /**
     * Distance calculator.
     */
    private final DistanceCalculator<U> dc;
    /**
     * Calculation mode.
     */
    private final ClosenessMode mode;

    /**
     * Basic constructor. Uses the harmonic mean computing algorithm.
     */
    public Closeness()
    {
        this(new FastDistanceCalculator<>(), ClosenessMode.COMPONENTS);
    }

    /**
     * Constructor.
     *
     * @param mode Computing algorithm to use.
     */
    protected Closeness(ClosenessMode mode)
    {
        this(new FastDistanceCalculator<>(), mode);
    }


    /**
     * Constructor. Uses the harmonic mean computing algorithm.
     *
     * @param dc distance calculator.
     */
    public Closeness(DistanceCalculator<U> dc)
    {
        this(dc, ClosenessMode.COMPONENTS);
    }

    /**
     * Constructor.
     *
     * @param dc   distance calculator.
     * @param mode computing algorithm.
     */
    protected Closeness(DistanceCalculator<U> dc, ClosenessMode mode)
    {
        this.dc = dc;
        this.mode = mode;
    }

    @Override
    public double compute(Graph<U> graph, U user)
    {
        this.dc.computeDistances(graph);
        double value = 0.0;
        switch (this.mode)
        {
            case HARMONICMEAN -> {
                value = this.dc.getDistancesFrom(user).values().stream().mapToDouble(dist -> (dist.isInfinite() || dist == 0) ? 0.0 : 1.0 / dist).sum();
                value /= (graph.getVertexCount() - 1.0);
            }
            case COMPONENTS -> {
                Communities<U> scc = this.dc.getSCC();
                int comm = scc.getCommunity(user);
                long numComm = scc.getUsers(comm).count();
                if (numComm > 1.0)
                {
                    value = (numComm - 1.0) / scc.getUsers(comm).mapToDouble(v -> this.dc.getDistances(user, v)).sum();
                }
            }
        }
        return value;
    }

    @Override
    public Map<U, Double> compute(Graph<U> graph)
    {
        Map<U, Double> closeness = new HashMap<>();
        graph.getAllNodes().forEach(u -> closeness.put(u, this.compute(graph, u)));
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
