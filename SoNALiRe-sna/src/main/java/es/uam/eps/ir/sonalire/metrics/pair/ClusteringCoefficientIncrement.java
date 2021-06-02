/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.metrics.pair;

import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.utils.datatypes.Pair;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Computes the increment of clustering coefficient if a link is added.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 */
public class ClusteringCoefficientIncrement<U> extends AbstractPairMetric<U>
{
    /**
     * The original network.
     */
    private Graph<U> graph = null;
    /**
     * The current number of triads.
     */
    private double numTriads = 0.0;
    /**
     * The current number of triangles
     */
    private double numTriangles = 0.0;
    /**
     * Incoming links.
     */
    private Map<U, Set<U>> ins = new HashMap<>();
    /**
     * Outgoing links.
     */
    private Map<U, Set<U>> outs = new HashMap<>();

    @Override
    public double compute(Graph<U> graph, U orig, U dest)
    {
        if (this.graph == null || this.graph != graph)
        {
            this.count(graph);
        }

        Set<U> uIn = ins.getOrDefault(orig, new HashSet<>());
        Set<U> uOut = outs.getOrDefault(orig, new HashSet<>());
        Set<U> vIn = ins.getOrDefault(dest, new HashSet<>());
        Set<U> vOut = outs.getOrDefault(dest, new HashSet<>());

        return this.computePair(graph, orig, dest, uIn, uOut, vIn, vOut);
    }

    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph)
    {
        if (this.graph == null || this.graph != graph)
        {
            this.count(graph);
        }

        Map<Pair<U>, Double> values = new HashMap<>();
        graph.getAllNodes().forEach(u ->
        {
            Set<U> uIn = ins.getOrDefault(u, new HashSet<>());
            Set<U> uOut = outs.getOrDefault(u, new HashSet<>());
            graph.getAllNodes().filter(v -> !v.equals(u)).forEach(v ->
            {
                Set<U> vIn = ins.getOrDefault(v, new HashSet<>());
                Set<U> vOut = outs.getOrDefault(v, new HashSet<>());
                double value = this.computePair(graph, u, v, uIn, uOut, vIn, vOut);
                values.put(new Pair<>(u, v), value);
            });
        });
        return values;
    }

    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph, Stream<Pair<U>> pairs)
    {
        if (this.graph == null || this.graph != graph)
        {
            this.count(graph);
        }


        Map<Pair<U>, Double> values = new ConcurrentHashMap<>();
        pairs.forEach(pair ->
        {
            U u = pair.v1();
            U v = pair.v2();
            Set<U> uIn = ins.getOrDefault(u, new HashSet<>());
            Set<U> uOut = outs.getOrDefault(u, new HashSet<>());
            Set<U> vIn = ins.getOrDefault(v, new HashSet<>());
            Set<U> vOut = outs.getOrDefault(v, new HashSet<>());
            double value = this.computePair(graph, u, v, uIn, uOut, vIn, vOut);
            values.put(pair, value);
        });

        return values;
    }

    /**
     * Computes the increment of the clustering coefficient for a pair of users.
     *
     * @param graph the original graph.
     * @param u     the origin user.
     * @param v     the destination user.
     * @param uIn   the incident nodes of the origin user.
     * @param uOut  the adjacent nodes of the origin user.
     * @param vIn   the incident nodes of the destination user.
     * @param vOut  the adjacent nodes of the destination user.
     *
     * @return the increment of clustering coefficient for the pair of users.
     */
    private double computePair(Graph<U> graph, U u, U v, Set<U> uIn, Set<U> uOut, Set<U> vIn, Set<U> vOut)
    {
        // If the graph does not change:
        if (u == v || graph.containsEdge(u, v))
        {
            return 0.0;
        }

        // Old value for the clustering coefficient.       
        double oldCC = (numTriads > 0.0) ? numTriangles / numTriads : 0.0;

        // Find the number of extra triads and triangles
        double extraTriplets = numTriads;
        double extraTriangles = numTriangles;

        // Case 1: The graph is directed
        if (graph.isDirected())
        {
            double countOutOut = 0.0;
            double countOutIn;
            double countInIn = 0.0;
            double uInSize = 0.0;
            double vOutSize = 0.0;

            for (U w : uIn)
            {
                if (w != u && w != v)
                {
                    if (vIn.contains(w))
                    {
                        countInIn++;
                    }
                    uInSize++;
                }
            }

            for (U w : vOut)
            {
                if (w != u && w != v)
                {
                    if (uOut.contains(w))
                    {
                        countOutOut++;
                    }
                    vOutSize++;
                }
            }

            countOutIn = uOut.stream().filter((w) -> (w != u && w != v && vIn.contains(w))).mapToDouble(w -> 1.0).sum();

            extraTriplets += uInSize + vOutSize;
            extraTriangles += countOutOut + countOutIn + countInIn;
        }
        else
        {
            double countInter = 0.0;
            double uSize = 0.0;
            double vSize;

            for (U w : uIn)
            {
                if (w != u && w != v)
                {
                    if (vIn.contains(w))
                    {
                        countInter++;
                    }
                    uSize++;
                }
            }

            vSize = vIn.stream().filter((w) -> (w != u && w != v)).mapToDouble(w -> 1.0).sum();

            extraTriplets += 2 * uSize + 2 * vSize;
            extraTriangles += 3 * countInter;
        }

        double newCC = (extraTriplets > 0.0) ? (extraTriangles) / (extraTriplets) : 0.0;

        return newCC - oldCC;
    }


    @Override
    public Function<U, Double> computeOrig(Graph<U> graph, U orig)
    {
        Object2DoubleOpenHashMap<U> triads = new Object2DoubleOpenHashMap<>();
        triads.defaultReturnValue(0.0);
        Object2DoubleOpenHashMap<U> triangles = new Object2DoubleOpenHashMap<>();
        triangles.defaultReturnValue(0.0);

        // First, if the variables are not initialized, do it.
        if (this.graph == null || !this.graph.equals(graph))
        {
            this.count(graph);
        }

        // Then, use the following algorithm:
        Set<U> uIn = ins.get(orig);
        Set<U> uOut = outs.get(orig);

        double uSize = 0.0;
        for (U w : uIn)
        {
            Set<U> wOut = outs.get(w);
            countTrianglesAndTriads(orig, triads, triangles, uOut, wOut);
            uSize++;
        }

        for (U w : uOut)
        {
            Set<U> wOut = outs.get(w);
            countTrianglesAndTriads(orig, triads, triangles, uOut, wOut);

            Set<U> wIn = ins.get(w);
            countTrianglesAndTriads(orig, triads, triangles, uOut, wIn);
        }


        return v -> ((this.numTriangles + triangles.getOrDefault(v, 0.0)) / (this.numTriads + triads.getOrDefault(v, 0.0))) - (this.numTriangles / this.numTriads);
    }

    /**
     * Auxiliar method for updating the number of triangles and triads.
     *
     * @param u         the origin node.
     * @param triads    the map for storing the number of triads.
     * @param triangles the map for storing the number of triangles.
     * @param uNeigh    the neighborhoods of u.
     * @param wNeigh    the neighborhoods of a neighbor of u.
     */
    private void countTrianglesAndTriads(U u, Object2DoubleOpenHashMap<U> triads, Object2DoubleOpenHashMap<U> triangles, Set<U> uNeigh, Set<U> wNeigh)
    {
        for (U v : wNeigh)
        {
            if (!v.equals(u) && !uNeigh.contains(v))
            {
                triangles.addTo(v, 1.0);
                if (!triads.containsKey(v))
                {
                    triads.put(v, outs.get(v).size());
                }
            }
        }
    }

    @Override
    public Function<U, Double> computeDest(Graph<U> graph, U dest)
    {
        return this.computeIndividual(graph, dest, false);
    }

    private Function<U, Double> computeIndividual(Graph<U> graph, U node, boolean orig)
    {
        Object2DoubleOpenHashMap<U> values = new Object2DoubleOpenHashMap<>();
        values.defaultReturnValue(0.0);

        if (this.graph == null || !this.graph.equals(graph))
        {
            this.count(graph);
        }

        Set<U> nodeIn = ins.get(node);
        Set<U> nodeOut = outs.get(node);


        graph.getAllNodes().filter(v -> orig ? !graph.containsEdge(node, v) : !graph.containsEdge(v, node)).forEach(v ->
        {
            Set<U> otherIn = ins.get(v);
            Set<U> otherOut = outs.get(v);
            double value;
            if (orig)
            {
                value = this.computePair(graph, node, v, nodeIn, nodeOut, otherIn, otherOut);
            }
            else
            {
                value = this.computePair(graph, v, node, otherIn, otherOut, nodeIn, nodeOut);
            }
            values.put(v, value);
        });

        return v -> values.getOrDefault(v, values.defaultReturnValue());
    }

    /**
     * Counts the number of triads and triangles in the graph.
     *
     * @param graph the graph.
     */
    private void count(Graph<U> graph)
    {
        this.ins = new HashMap<>();
        this.outs = new HashMap<>();


        AtomicInteger triadCounter = new AtomicInteger(0);
        AtomicInteger triangleCounter = new AtomicInteger(0);
        graph.getAllNodes().forEach((u) ->
        {
            ins.put(u, new HashSet<>());
            outs.put(u, new HashSet<>());
            graph.getIncidentNodes(u).forEach((v) ->
            {
                ins.get(u).add(v);
                graph.getAdjacentNodes(u).forEach((w) ->
                {
                    outs.get(u).add(w);
                    if (!w.equals(v) && !u.equals(v) && !u.equals(w))
                    {
                        triadCounter.incrementAndGet();
                        if (graph.containsEdge(v, w))
                        {
                            triangleCounter.incrementAndGet();
                        }
                    }
                });
            });
        });

        this.numTriads = triadCounter.get();
        this.numTriangles = triangleCounter.get();
        this.graph = graph;
    }

}
