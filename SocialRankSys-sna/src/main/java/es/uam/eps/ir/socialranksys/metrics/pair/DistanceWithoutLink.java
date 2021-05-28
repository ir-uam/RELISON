/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.pair;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.socialranksys.metrics.distance.FastDistanceCalculator;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Computes the distance between two nodes in the network, considering that the link does not exist.
 * If there is no link, the usual distance is computed.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class DistanceWithoutLink<U> extends AbstractPairMetric<U>
{
    /**
     * Distance calculator.
     */
    private final DistanceCalculator<U> dc;

    /**
     * Constructor.
     *
     * @param dc distance calculator.
     */
    public DistanceWithoutLink(DistanceCalculator<U> dc)
    {
        this.dc = dc;
    }

    /**
     * Constructor.
     */
    public DistanceWithoutLink()
    {
        this(new FastDistanceCalculator<>());
    }


    @Override
    public double compute(Graph<U> graph, U orig, U dest)
    {
        if (!graph.containsVertex(orig) || !graph.containsVertex(dest))
        {
            return Double.NaN;
        }

        if (!graph.containsEdge(orig, dest))
        {
            this.dc.computeDistances(graph);
            return this.dc.getDistances(orig, dest);
        }

        // Otherwise:
        Set<U> neighbors = graph.getIncidentNodes(dest).filter(v -> v != orig).collect(Collectors.toCollection(HashSet::new));

        Queue<U> queue = new LinkedList<>();
        Queue<U> nextLevelQueue = new LinkedList<>();

        queue.add(orig);
        AtomicInteger d = new AtomicInteger();
        d.set(0);

        while (!queue.isEmpty())
        {
            U current = queue.poll();

            if (neighbors.contains(current))
            {
                return d.get() + 1.0;
            }
            else
            {
                graph.getAdjacentNodes(current).filter(v -> v != dest).forEach(nextLevelQueue::add);
            }

            if (queue.isEmpty())
            {
                while (!nextLevelQueue.isEmpty())
                {
                    queue.add(nextLevelQueue.poll());
                }
                d.incrementAndGet();
            }
        }

        // If the node is not found
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph)
    {
        Map<Pair<U>, Double> map = new HashMap<>();
        graph.getAllNodes().forEach(u ->
            graph.getAllNodes().forEach(v ->
                map.put(new Pair<>(u, v), this.compute(graph, u, v))
            )
        );
        return map;
    }

    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph, Stream<Pair<U>> pairs)
    {
        Map<Pair<U>, Double> map = new ConcurrentHashMap<>();
        pairs.forEach(pair -> map.put(pair, this.compute(graph, pair.v1(), pair.v2())));
        return map;
    }

    @Override
    public double averageValue(Graph<U> graph)
    {
        Map<Pair<U>, Double> values = this.compute(graph);

        double value = 0.0;

        for (Entry<Pair<U>, Double> entry : values.entrySet())
        {
            if (!entry.getValue().isInfinite())
            {
                value += entry.getValue();
            }
        }

        if (graph.getVertexCount() >= 1)
        {
            return value / (graph.getVertexCount() * graph.getVertexCount());
        }
        return 0.0;
    }

    @Override
    public double averageValue(Graph<U> graph, Stream<Pair<U>> pairs, int pairCount)
    {
        Map<Pair<U>, Double> values = this.compute(graph, pairs);

        double value = 0.0;
        for (Entry<Pair<U>, Double> entry : values.entrySet())
        {
            if (!entry.getValue().isInfinite())
            {
                value += entry.getValue();
            }
        }

        if (values.size() >= 1)
        {
            return value / (values.size());
        }
        return 0.0;
    }


}
