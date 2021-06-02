/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.metrics.distance.graph;

import es.uam.eps.ir.sonalire.community.Communities;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.metrics.GraphMetric;
import es.uam.eps.ir.sonalire.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.sonalire.metrics.distance.FastDistanceCalculator;
import es.uam.eps.ir.sonalire.metrics.distance.modes.ASLMode;

import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Computes the Average Shortest path Length of graphs.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class ASL<U> implements GraphMetric<U>
{
    /**
     * Calculation mode
     */
    private final ASLMode mode;
    /**
     * Calculates the distances
     */
    private final DistanceCalculator<U> dc;

    /**
     * Constructor
     *
     * @param dc   distance calculator.
     * @param mode the calculation mode.
     */
    public ASL(DistanceCalculator<U> dc, ASLMode mode)
    {
        this.mode = mode;
        this.dc = dc;
    }

    /**
     * Constructor. Applies the default mode (Averages over all the pairs of distinct nodes without infinite distances
     *
     * @param dc distance calculator.
     */
    public ASL(DistanceCalculator<U> dc)
    {
        this(dc, ASLMode.NONINFINITEDISTANCES);
    }

    /**
     * Constructor
     *
     * @param mode the calculation mode.
     */
    public ASL(ASLMode mode)
    {
        this(new FastDistanceCalculator<>(), mode);
    }

    /**
     * Default constructor. Applies the default mode (Averages over all the pairs of distinct nodes without infinite distances
     */
    public ASL()
    {
        this(new FastDistanceCalculator<>(), ASLMode.NONINFINITEDISTANCES);
    }

    @Override
    public double compute(Graph<U> graph)
    {
        if (graph.getVertexCount() <= 1L || graph.getEdgeCount() == 0L)
        {
            return 0.0;
        }

        this.dc.computeDistances(graph);
        double asl = -1.0;
        AtomicInteger counter = new AtomicInteger();
        counter.set(0);
        switch (this.mode)
        {
            case NONINFINITEDISTANCES -> // Averages over the pairs of distinct nodes without infinite distances
                    asl = this.dc.getASL();
            case COMPONENTS ->
            { // Computes the metric for each strongly connected component, and averages
                Communities<U> scc = this.dc.getSCC();
                OptionalDouble optional = scc.getCommunities().mapToDouble(comm ->
                {
                    long users = scc.getUsers(comm).count();
                    double aux = scc.getUsers(comm).mapToDouble(u -> scc.getUsers(comm).mapToDouble(v -> this.dc.getDistances(u, v)).sum()).sum();
                    if (users > 1L)
                    {
                        return aux / ((users + 0.0) * (users - 1.0));
                    }
                    return 0;
                }).average();

                asl = optional.isPresent() ? optional.getAsDouble() : 0.0;
            }
        }

        return asl;
    }

}
