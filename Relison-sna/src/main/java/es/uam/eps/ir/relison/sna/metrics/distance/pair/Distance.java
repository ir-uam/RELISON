/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.sna.metrics.distance.pair;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.sna.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.relison.sna.metrics.distance.FastDistanceCalculator;
import es.uam.eps.ir.relison.sna.metrics.distance.modes.ASLMode;
import es.uam.eps.ir.relison.sna.metrics.pair.AbstractPairMetric;
import es.uam.eps.ir.relison.utils.datatypes.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Computes the distance between nodes.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Distance<U> extends AbstractPairMetric<U>
{
    /**
     * Distance calculator.
     */
    private final DistanceCalculator<U> dc;

    /**
     * Limits the pairs of nodes over which the metric will be averaged. By default, values are averaged
     * over pairs at finite distance.
     */
    private final ASLMode aslMode;

    /**
     * Constructor.
     *
     * @param dc distance calculator
     */
    public Distance(DistanceCalculator<U> dc)
    {
        this.dc = dc;
        this.aslMode = ASLMode.NONINFINITEDISTANCES;
    }

    /**
     * Constructor.
     */
    public Distance()
    {
        this.dc = new FastDistanceCalculator<>();
        this.aslMode = ASLMode.NONINFINITEDISTANCES;
    }

    /**
     * Constructor.
     *
     * @param aslMode limits the pairs of nodes over which the metric will be averaged.
     */
    public Distance(ASLMode aslMode)
    {
        this.dc = new FastDistanceCalculator<>();
        this.aslMode = aslMode;
    }

    /**
     * Constructor.
     *
     * @param dc      distance calculator
     * @param aslMode limits the pairs of nodes over which the metric will be averaged.
     */
    public Distance(DistanceCalculator<U> dc, ASLMode aslMode)
    {
        this.dc = dc;
        this.aslMode = aslMode;
    }

    @Override
    public double compute(Graph<U> graph, U orig, U dest)
    {
        this.dc.computeDistances(graph);
        return dc.getDistances(orig, dest);
    }

    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph)
    {
        Map<Pair<U>, Double> values = new HashMap<>();
        dc.computeDistances(graph);
        graph.getAllNodes().forEach(u -> graph.getAllNodes().forEach(v ->
        {
            Pair<U> pair = new Pair<>(u, v);
            values.put(pair, dc.getDistances(u, v));
        }));

        return values;
    }

    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph, Stream<Pair<U>> pairs)
    {
        Map<Pair<U>, Double> values = new ConcurrentHashMap<>();
        dc.computeDistances(graph);
        pairs.forEach(pair -> values.put(pair, dc.getDistances(pair.v1(), pair.v2())));
        return values;
    }

    @Override
    public double averageValue(Graph<U> graph)
    {
        OptionalDouble opt = this.compute(graph).values().stream().mapToDouble(val -> val).average();
        return opt.isPresent() ? opt.getAsDouble() : Double.NaN;
    }

    @Override
    public double averageValue(Graph<U> graph, Stream<Pair<U>> pairs, int pairCount)
    {
        dc.computeDistances(graph);

        if (this.aslMode.equals(ASLMode.NONINFINITEDISTANCES))
        {
            double value = 0.0;
            List<Pair<U>> pairsList = pairs.collect(Collectors.toCollection(ArrayList::new));
            double numPairs = 0.0;
            for (Pair<U> pair : pairsList)
            {
                double distance = dc.getDistances(pair.v1(), pair.v2());
                if (!Double.isInfinite(distance) && !Double.isNaN(distance))
                {
                    value += distance;
                    numPairs += 1.0;
                }

            }

            if (numPairs == 0.0)
            {
                return value;
            }
            else
            {
                return value / numPairs;
            }
        }
        else // Nodes inside SCC Components
        {
            double value = 0.0;
            List<Pair<U>> pairsList = pairs.collect(Collectors.toCollection(ArrayList::new));
            double numPairs = 0.0;
            for (Pair<U> pair : pairsList)
            {
                if (dc.getSCC().getCommunity(pair.v1()) == dc.getSCC().getCommunity(pair.v2()))
                {
                    double distance = dc.getDistances(pair.v1(), pair.v2());
                    value += distance;
                    numPairs += 1.0;
                }
            }

            if (numPairs == 0.0)
            {
                return value;
            }
            else
            {
                return value / numPairs;
            }
        }
    }
}
