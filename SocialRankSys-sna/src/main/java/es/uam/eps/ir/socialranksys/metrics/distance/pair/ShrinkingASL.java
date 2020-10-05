/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.distance.pair;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.socialranksys.metrics.distance.FastDistanceCalculator;
import es.uam.eps.ir.socialranksys.metrics.pair.AbstractPairMetric;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Computes the variation of the average shortest path length if a link is
 * included in the graph.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class ShrinkingASL<U> extends AbstractPairMetric<U>
{
    /**
     * Distance calculator.
     */
    private final DistanceCalculator<U> dc;

    /**
     * Constructor
     */
    public ShrinkingASL()
    {
        dc = new FastDistanceCalculator<>();
    }

    /**
     * Constructor.
     *
     * @param dc distance calculator.
     */
    public ShrinkingASL(DistanceCalculator<U> dc)
    {
        this.dc = dc;
    }

    @Override
    public double compute(Graph<U> graph, U orig, U dest)
    {

        // First, we find the pairs of users at maximum distance (diameter)
        this.dc.computeDistances(graph);

        Map<U, Map<U, Double>> distances = this.dc.getDistances();
        Map<U, Set<U>> cins = new HashMap<>();
        Map<U, Set<U>> couts = new HashMap<>();
        TreeMap<Double, List<Pair<U>>> pairsAtDistance = new TreeMap<>();

        // First, find the old ASL.
        double sum = 0.0;
        double count = 0.0;
        for (U u : distances.keySet())
        {
            for (U v : distances.get(u).keySet())
            {
                double distance = distances.get(u).get(v);
                if (Double.isFinite(distance))
                {
                    if (!cins.containsKey(v))
                    {
                        cins.put(v, new HashSet<>());
                    }
                    if (!couts.containsKey(u))
                    {
                        couts.put(u, new HashSet<>());
                    }
                    cins.get(v).add(u);
                    couts.get(u).add(v);
                    sum += distance;
                    count++;
                }
            }
        }
        Set<U> uCIn = cins.getOrDefault(orig, new HashSet<>());
        Set<U> vCOut = couts.getOrDefault(dest, new HashSet<>());

        return this.computePair(graph, orig, dest, uCIn, vCOut, distances, sum, count);
    }

    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph)
    {
        // First, we find the pairs of users at maximum distance (diameter)
        this.dc.computeDistances(graph);

        Map<U, Map<U, Double>> distances = this.dc.getDistances();
        Map<U, Set<U>> cins = new HashMap<>();
        Map<U, Set<U>> couts = new HashMap<>();
        TreeMap<Double, List<Pair<U>>> pairsAtDistance = new TreeMap<>();

        // First, find the old ASL.
        double sum = 0.0;
        double count = 0.0;
        for (U u : distances.keySet())
        {
            for (U v : distances.get(u).keySet())
            {
                double distance = distances.get(u).get(v);
                if (Double.isFinite(distance))
                {
                    if (!cins.containsKey(v))
                    {
                        cins.put(v, new HashSet<>());
                    }
                    if (!couts.containsKey(u))
                    {
                        couts.put(u, new HashSet<>());
                    }
                    cins.get(v).add(u);
                    couts.get(u).add(v);
                    sum += distance;
                    count++;
                }
            }
        }

        double defsum = sum;
        double defcount = count;

        double oldasl = count > 0.0 ? sum / count : 0.0;
        Map<Pair<U>, Double> values = new HashMap<>();

        // Then, for each pair of users in the network, find the value of the metric
        graph.getAllNodes().forEach(u ->
        {
            Set<U> uCIn = cins.getOrDefault(u, new HashSet<>());
            graph.getAllNodes().forEach(v ->
            {
                Set<U> vCOut = couts.getOrDefault(v, new HashSet<>());
                double value = computePair(graph, u, v, uCIn, vCOut, distances, defsum, defcount);
                values.put(new Pair<>(u, v), value);
            });
        });

        return values;
    }

    /**
     * Computes the metric for an individual pair of users.
     *
     * @param graph     the graph.
     * @param u         the origin user.
     * @param v         the destination user.
     * @param uCIn      the incoming component of user u
     * @param vCOut     the outgoing component of user v
     * @param distances the distances.
     * @param sum       the sum of finite distances
     * @param count     the number of pairs connected by finite distances.
     *
     * @return the value of the metric.
     */
    private double computePair(Graph<U> graph, U u, U v, Set<U> uCIn, Set<U> vCOut, Map<U, Map<U, Double>> distances, double sum, double count)
    {
        // If the edge already exists, nothing is modified.
        if (graph.containsEdge(u, v))
        {
            return 0.0;
        }
        else if (u.equals(v))
        {
            return 0.0;
        }

        // Find the old ASL and clone the sum and count values.
        double asl = (count > 0.0) ? sum / count : 0.0;
        double auxsum = sum;
        double auxcount = count;

        // Then, study the case when ASL changes:        
        // Case 1: the graph is directed
        if (graph.isDirected())
        {
            // First, d(u,v) = 1
            double uvdist = distances.get(u).get(v);
            if (!Double.isFinite(uvdist))
            {
                auxsum++;
                auxcount++;
            }
            else
            {
                auxsum += (1.0 - uvdist);
            }

            // Then, for each w1 in the in-component of u
            for (U w1 : uCIn)
            {
                if (!w1.equals(u))
                {
                    // Update distances from w1 to v
                    double distToV = distances.get(w1).get(v);
                    double newDistToV = distances.get(w1).get(u) + 1.0;

                    if (!Double.isFinite(distToV)) // We just add the new pair to the ASL list.
                    {
                        auxsum += newDistToV;
                        auxcount++;
                    }
                    else
                    {
                        auxsum += (Math.min(distToV, newDistToV) - distToV);
                    }

                    // Update distances from w1 to w2 in the out-component of v
                    for (U w2 : vCOut)
                    {
                        if (!w2.equals(v) && !w1.equals(w2))
                        {
                            double dist = distances.get(w1).get(w2);
                            double newDist = distances.get(w1).get(u) + 1.0 + distances.get(v).get(w2);

                            if (!Double.isFinite(dist))
                            {
                                auxsum += newDist;
                                auxcount++;
                            }
                            else
                            {
                                auxsum += (Math.min(dist, newDist) - dist);
                            }
                        }
                    }
                }
            }

            // For each user in the out-component of v, find the new distances from u
            for (U w2 : vCOut)
            {
                if (w2 != v)
                {
                    double distFromU = distances.get(u).get(w2);
                    double newDistFromU = distances.get(v).get(w2) + 1.0;

                    if (!Double.isFinite(distFromU))
                    {
                        auxsum += newDistFromU;
                        auxcount++;
                    }
                    else
                    {
                        auxsum += Math.min(distFromU, newDistFromU) - distFromU;
                    }
                }
            }
        }
        else // Case 2: the graph is undirected
        {
            // First, d(u,v) = 1
            double uvdist = distances.get(u).get(v);
            if (!Double.isFinite(uvdist))
            {
                auxsum += 2;
                auxcount += 2;
            }
            else
            {
                auxsum += 2.0 * (1.0 - uvdist);
            }

            for (U w1 : uCIn)
            {
                if (!w1.equals(u) && !w1.equals(v))
                {
                    double distV = distances.get(w1).get(v);
                    double newDistV = distances.get(w1).get(u) + 1.0;

                    if (!Double.isFinite(distV))
                    {
                        auxsum += 2.0 * newDistV;
                        auxcount += 2.0;
                    }
                    else
                    {
                        auxsum += 2.0 * (Math.min(distV, newDistV) - distV);
                    }

                    for (U w2 : vCOut)
                    {
                        if (!w2.equals(v) && !w2.equals(u) && !w1.equals(w2))
                        {
                            double dist = distances.get(w1).get(w2);
                            double newDist = distances.get(w1).get(u) + 1.0 + distances.get(v).get(w2);

                            if (!Double.isFinite(dist))
                            {
                                auxsum += 2.0 * newDist;
                                auxcount += 2.0;
                            }
                            else
                            {
                                auxsum += (Math.min(dist, newDist) - dist);
                            }
                        }
                    }
                }
            }

            for (U w2 : vCOut)
            {
                if (!w2.equals(u) && !w2.equals(v))
                {
                    double distU = distances.get(u).get(w2);
                    double newDistU = distances.get(v).get(w2) + 1.0;

                    if (!Double.isFinite(distU))
                    {
                        auxsum += newDistU;
                        auxcount++;
                    }
                    else
                    {
                        auxsum += Math.min(distU, newDistU) - distU;
                    }
                }
            }
        }

        double auxasl = (auxcount > 0.0) ? auxsum / auxcount : 0.0;
        return asl - auxasl;
    }


    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph, Stream<Pair<U>> pairs)
    {
        this.dc.computeDistances(graph);

        Map<U, Map<U, Double>> distances = this.dc.getDistances();
        Map<U, Set<U>> cins = new HashMap<>();
        Map<U, Set<U>> couts = new HashMap<>();
        TreeMap<Double, Long> counter = new TreeMap<>();
        TreeMap<Double, List<Pair<U>>> pairsAtDistance = new TreeMap<>();
        double sum = 0.0;
        double count = 0.0;

        // First, precompute the necessary objects:
        // a) distances between pairs of users.
        // b) in-components of nodes.
        // c) out-components of nodes.
        // d) a counter for the number of pairs at each distance
        // e) the different pairs at each distance.
        // f) the sum of the finite distances.
        // g) the number of finite distances.
        for (U u : distances.keySet())
        {
            for (U v : distances.get(u).keySet())
            {
                double distance = distances.get(u).get(v);
                if (Double.isFinite(distance) && !u.equals(v))
                {
                    if (!pairsAtDistance.containsKey(distance))
                    {
                        pairsAtDistance.put(distance, new ArrayList<>());
                        counter.put(distance, 0L);
                    }
                    if (!cins.containsKey(v))
                    {
                        cins.put(v, new HashSet<>());
                    }
                    if (!couts.containsKey(u))
                    {
                        couts.put(u, new HashSet<>());
                    }

                    pairsAtDistance.get(distance).add(new Pair<>(u, v));
                    counter.put(distance, counter.get(distance) + 1L);

                    cins.get(v).add(u);
                    couts.get(u).add(v);
                    sum += distance;
                    count++;
                }
            }
        }

        double defsum = sum;
        double defcount = count;
        Map<Pair<U>, Double> values = new ConcurrentHashMap<>();
        pairs.forEach(pair ->
        {
            U u = pair.v1();
            U v = pair.v2();
            Set<U> uCIn = cins.getOrDefault(u, new HashSet<>());
            Set<U> uCOut = couts.getOrDefault(u, new HashSet<>());
            Set<U> vCOut = couts.getOrDefault(v, new HashSet<>());
            Set<U> vCIn = cins.getOrDefault(v, new HashSet<>());
            double value = this.computePair(graph, u, v, uCIn, vCOut, uCOut, vCIn, distances, pairsAtDistance, counter, defsum, defcount);
            values.put(pair, value);
        });

        return values;
        /*
        // First, we find the pairs of users at maximum distance (diameter)
        this.dc.computeDistances(graph);
        
        Map<U, Map<U, Double>> distances = this.dc.getDistances();
        Map<U, Set<U>> cins = new HashMap<>();
        Map<U, Set<U>> couts = new HashMap<>();
        
        // First, find the old ASL.
        double sum = 0.0;
        double count = 0.0;
        for(U u : distances.keySet())
        {
            for(U v : distances.get(u).keySet())
            {
                double distance = distances.get(u).get(v);
                if(Double.isFinite(distance) && !u.equals(v))
                {
                    if(!cins.containsKey(v)) cins.put(v, new HashSet<>());
                    if(!couts.containsKey(u)) couts.put(u, new HashSet<>());
                    cins.get(v).add(u);
                    couts.get(u).add(v);
                    sum += distance;
                    count++;
                }
            }
        }
        double defsum = sum;
        double defcount = count;
        Map<Pair<U>, Double> values = new HashMap<>();
        pairs.forEach(pair -> 
        {
            U u = pair.v1();
            U v = pair.v2();
            Set<U> uCIn = cins.getOrDefault(u, new HashSet<>());
            Set<U> vCOut = couts.getOrDefault(v, new HashSet<>());
            double value = this.computePair(graph, u, v, uCIn, vCOut, distances, defsum, defcount);
            values.put(pair, value);
        });
        
        return values;*/
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
        OptionalDouble opt = values.values().stream().mapToDouble(x -> x).average();
        return opt.isPresent() ? opt.getAsDouble() : Double.NaN;
    }

    /**
     * Given a pair, computes the value of the metric.
     *
     * @param graph           the original graph.
     * @param u               the origin user.
     * @param v               the destination user.
     * @param uCIn            the in-component of the origin user.
     * @param vCOut           the out-component of the destination user.
     * @param uCOut           the out-component of the origin user
     * @param vCIn            the in-component of the destination user
     * @param distances       a map containing the distances between pairs.
     * @param pairsAtDistance a map ccontaining the pairs at a given distance.
     * @param counter         counts how many pairs there are at a given distance.
     * @param sum             sum of the finite distances between pairs in the original graph.
     * @param count           number of finite distances between pairs in the original graph.
     *
     * @return the value of the metric for that pair.
     */
    private double computePair(Graph<U> graph, U u, U v, Set<U> uCIn, Set<U> vCOut, Set<U> uCOut, Set<U> vCIn, Map<U, Map<U, Double>> distances, TreeMap<Double, List<Pair<U>>> pairsAtDistance, TreeMap<Double, Long> counter, double sum, double count)
    {
        if (graph.isDirected())
        {
            return this.computeDirected(graph, u, v, uCIn, vCOut, uCOut, vCIn, distances, pairsAtDistance, counter, sum, count);
        }
        else
        {
            return this.computeUndirected(graph, u, v, uCIn, vCOut, distances, pairsAtDistance, counter, sum, count);
        }
    }

    /**
     * Given a pair, computes the value of the metric in case the graph is directed.
     *
     * @param graph           the original graph.
     * @param u               the origin user.
     * @param v               the destination user.
     * @param uCIn            the in-component of the origin user.
     * @param vCOut           the out-component of the destination user.
     * @param uCOut           the out-component of the origin user.
     * @param vCIn            the in-component of the destination user.
     * @param distances       a map containing the distances between pairs.
     * @param pairsAtDistance a map ccontaining the pairs at a given distance.
     * @param counter         counts how many pairs there are at a given distance.
     * @param sum             sum of the finite distances between pairs in the original graph.
     * @param count           number of finite distances between pairs in the original graph.
     *
     * @return the value of the metric for that pair.
     */
    private double computeDirected(Graph<U> graph, U u, U v, Set<U> uCIn, Set<U> vCOut, Set<U> uCOut, Set<U> vCIn, Map<U, Map<U, Double>> distances, TreeMap<Double, List<Pair<U>>> pairsAtDistance, TreeMap<Double, Long> counter, double sum, double count)
    {
        // If the graph already contains the edge, nothing changes.
        if (graph.containsEdge(u, v))
        {
            return 0.0;
        }
        double oldasl = (count > 0.0) ? sum / count : 0.0;

        double auxSum = sum;
        double auxCount = count;

        // Obtain the difference between the previous distance between u and v and the new one.
        double distUV = distances.get(u).get(v);
        if (Double.isFinite(distUV))
        {
            auxSum += (1.0 - distUV);
        }
        else
        {
            auxSum += 1.0;
            auxCount += 1.0;
        }

        // Now, study the possible cases:
        // Case 1: u is not a source and v is not a sink -> GENERAL CASE
        if (!uCIn.isEmpty() && !vCOut.isEmpty())
        {
            // first, we copy the counter map.
            TreeMap<Double, Long> auxCounter = new TreeMap<>(counter);
            // Modify the value for the directed graph.
            if (Double.isFinite(distUV))
            {
                auxCounter.put(distUV, auxCounter.get(distUV) - 1L);
                if (auxCounter.get(distUV) == 0)
                {
                    auxCounter.remove(distUV);
                }
            }

            // Then, check the distances between the incoming neighborhood of u and v:
            Pair<Double> pair = graph.getIncidentNodes(u).map(w ->
            {
                double distWV = distances.get(w).get(v);
                double auxDist = 2.0;
                double counterIncr = 0.0;
                if (Double.isFinite(distWV))
                {
                    auxDist = Math.min(distWV, auxDist);
                    if (auxDist != distWV)
                    {
                        auxCounter.put(distWV, auxCounter.get(distWV) - 1L);
                    }
                    if (auxCounter.get(distWV) == 0)
                    {
                        auxCounter.remove(distWV);
                    }
                    auxDist -= distWV;
                }
                else
                {
                    counterIncr = 1.0;
                }

                return new Pair<>(auxDist, counterIncr);
            }).reduce(new Pair<>(0.0, 0.0), (x, y) -> new Pair<>(x.v1() + y.v1(), x.v2() + y.v2()));

            auxSum += pair.v1();
            auxCount += pair.v2();

            // Then, check the distances between the outgoing neighborhood of v and u:
            pair = graph.getAdjacentNodes(v).map(w ->
            {
                double distUW = distances.get(u).get(w);
                double auxDist = 2.0;
                double counterIncr = 0.0;
                if (Double.isFinite(distUW))
                {
                    auxDist = Math.min(distUW, auxDist);
                    if (auxDist != distUW)
                    {
                        auxCounter.put(distUW, auxCounter.get(distUW) - 1L);
                    }
                    if (auxCounter.get(distUW) == 0)
                    {
                        auxCounter.remove(distUW);
                    }
                    auxDist -= distUW;
                }
                else
                {
                    counterIncr = 1.0;
                }

                return new Pair<>(auxDist, counterIncr);
            }).reduce(new Pair<>(0.0, 0.0), (x, y) -> new Pair<>(x.v1() + y.v1(), x.v2() + y.v2()));

            auxSum += pair.v1();
            auxCount += pair.v2();
            // Then, starting with pairs at distance 4...
            double top = counter.lastKey();
            Double current = 3.0;
            while ((current = counter.ceilingKey(current + 1.0)) != null)
            {
                if (!auxCounter.containsKey(current))
                {
                    break;
                }

                double distWW = current;
                auxSum += pairsAtDistance.get(current).stream().mapToDouble(p ->
                {
                    U w1 = p.v1();
                    U w2 = p.v2();

                    if (!w1.equals(u) && !w2.equals(v))
                    {
                        if (uCIn.contains(w1) && vCOut.contains(w2))
                        {
                            double auxDist = Math.min(distances.get(w1).get(u) + 1.0 + distances.get(v).get(w2), distWW);
                            return auxDist - distWW;
                        }
                        return 0.0;
                    }
                    else if (!w1.equals(u) && w2.equals(v))
                    {
                        if (uCIn.contains(w1) && distances.get(w1).get(u) > 1)
                        {
                            double auxDist = Math.min(distances.get(w1).get(u) + 1.0, distWW);
                            return auxDist - distWW;
                        }
                        return 0.0;
                    }
                    else if (w1.equals(u) && !w2.equals(v))
                    {
                        if (vCOut.contains(w2) && distances.get(v).get(w2) > 1)
                        {
                            double auxDist = Math.min(1.0 + distances.get(v).get(w2), distWW);
                            return auxDist - distWW;
                        }
                        return 0.0;
                    }
                    return 0.0;
                }).sum();
            }

            // Now, only pairs at distance equal to infinity remain.
            if (!Double.isFinite(distUV))
            {
                // Infinite from u.
                Set<U> infiniteFromU = new HashSet<>(vCOut);
                infiniteFromU.removeAll(uCOut);
                for (U w : infiniteFromU)
                {
                    // First, update distances from u.
                    double distVW = distances.get(v).get(w);
                    if (distVW > 1)
                    {
                        auxCount++;
                        auxSum += distVW + 1.0;
                    }

                    // Now, update distances from the incoming component of u to w.
                    for (U w2 : uCIn)
                    {
                        double distWW = distances.get(w2).get(w);
                        if (!Double.isFinite(distWW)) // if it has not been already studied
                        {
                            // (u,v) represents the only access point from v to u.
                            auxCount++;
                            auxSum += distVW + 1.0 + distances.get(w2).get(u);
                        }
                    }
                }

                // Infinite distance to v
                Set<U> infiniteToV = new HashSet<>(uCIn);
                infiniteToV.removeAll(vCOut);
                for (U w : infiniteToV)
                {
                    // First, update distances to v.
                    double distWU = distances.get(w).get(u);
                    if (distWU > 1)
                    {
                        auxCount++;
                        auxSum += distWU + 1.0;
                    }

                    // Next, update distances to the outgoing component of v.
                    for (U w2 : vCOut)
                    {
                        double distWW = distances.get(w2).get(w);
                        if (!Double.isFinite(distWW)) // if it was finite, already computed.
                        {
                            auxCount++;
                            auxSum += distWU + 1.0 + distances.get(w2).get(v);
                        }
                    }
                }
            }
        }
        // Case 2: u is a source, v is not a sink -> update distances from u to Cout(v)
        else if (!vCOut.isEmpty())
        {
            for (U w : vCOut)
            {
                if (w != v)
                {
                    double distUW = distances.get(u).get(w);
                    double auxDist = distances.get(v).get(w) + 1.0;
                    if (Double.isFinite(distUW))
                    {
                        auxDist = Math.min(distUW, auxDist);
                        auxSum += auxDist - distUW;
                    }
                    else
                    {
                        auxSum += auxDist;
                        auxCount += 1.0;
                    }
                }
            }
        }
        // Case 3: v is a sink, u is not a source
        else if (!uCIn.isEmpty())
        {
            for (U w : uCIn)
            {
                if (w != u)
                {
                    double distWV = distances.get(w).get(v);
                    double auxDist = distances.get(w).get(u) + 1.0;
                    if (Double.isFinite(distWV))
                    {
                        auxDist = Math.min(distWV, auxDist);
                        auxSum += auxDist - distWV;
                    }
                    else
                    {
                        auxSum += auxDist;
                        auxCount += 1.0;
                    }
                }
            }
        }

        return oldasl - auxSum / auxCount;
    }

    /**
     * Given a pair, computes the value of the metric in case the graph is undirected.
     *
     * @param graph           the original graph.
     * @param u               the origin user.
     * @param v               the destination user.
     * @param uCIn            the in-component of the origin user.
     * @param vCOut           the out-component of the destination user.
     * @param distances       a map containing the distances between pairs.
     * @param pairsAtDistance a map ccontaining the pairs at a given distance.
     * @param counter         counts how many pairs there are at a given distance.
     * @param sum             sum of the finite distances between pairs in the original graph.
     * @param count           number of finite distances between pairs in the original graph.
     *
     * @return the value of the metric for that pair.
     */
    private double computeUndirected(Graph<U> graph, U u, U v, Set<U> uCIn, Set<U> vCOut, Map<U, Map<U, Double>> distances, TreeMap<Double, List<Pair<U>>> pairsAtDistance, TreeMap<Double, Long> counter, double sum, double count)
    {
        double oldasl = (count > 0) ? sum / count : 0.0;
        double auxSum = sum;
        double auxCount = count;

        // First, update link from u to v:
        double distUV = distances.get(u).get(v);
        if (Double.isFinite(distUV))
        {
            auxSum += 2 * (1.0 - distUV);
        }
        else
        {
            auxSum += 2.0;
            auxCount += 2 * count;
        }

        // Case 1: General case:
        if (!uCIn.isEmpty() && !vCOut.isEmpty())
        {
            // If the distance between u and v is infinite, u and v belong to different connected components
            // Therefore, we only need to update distances between the users in both
            // components ----> faster.
            if (!Double.isFinite(distUV))
            {
                for (U w1 : uCIn)
                {
                    double distWU = distances.get(w1).get(u);
                    auxSum += 2 * (distWU + 1.0);
                    auxCount += 2.0;
                    if (!w1.equals(u))
                    {
                        for (U w2 : vCOut)
                        {
                            double distWV = distances.get(w2).get(v);
                            auxSum += 2 * (distWU + distWV + 1.0);
                            auxCount += 2.0;
                        }
                    }
                }

                for (U w2 : vCOut)
                {
                    double distWV = distances.get(w2).get(v);
                    auxSum += 2 * (distWV + 1.0);
                    auxCount += 2.0;
                }
            }
            else
            {
                // first, we copy the counter map.
                TreeMap<Double, Long> auxCounter = new TreeMap<>(counter);
                // Modify the value for the directed graph.
                if (Double.isFinite(distUV))
                {
                    auxCounter.put(distUV, auxCounter.get(distUV) - 2L);
                    if (auxCounter.get(distUV) == 0)
                    {
                        auxCounter.remove(distUV);
                    }
                }

                // Then, check the distances between the neighborhood of u and v:
                Pair<Double> pair = graph.getNeighbourNodes(u).map(w ->
                {
                    double distWV = distances.get(w).get(v);
                    double auxDist = 2.0;
                    double counterIncr = 0.0;
                    if (Double.isFinite(distWV))
                    {
                        auxDist = Math.min(distWV, auxDist);
                        if (auxDist != distWV)
                        {
                            auxCounter.put(distWV, auxCounter.get(distWV) - 2L);
                        }
                        if (auxCounter.get(distWV) == 0)
                        {
                            auxCounter.remove(distWV);
                        }
                        auxDist -= distWV;
                    }
                    else
                    {
                        counterIncr = 1.0;
                    }

                    return new Pair<>(auxDist, counterIncr);
                }).reduce(new Pair<>(0.0, 0.0), (x, y) -> new Pair<>(x.v1() + y.v1(), x.v2() + y.v2()));
                auxSum += 2 * pair.v1();
                auxCount += 2 * pair.v2();

                // Then, check the distances between the neighborhood of v and u:
                pair = graph.getNeighbourNodes(v).map(w ->
                {
                    double distWU = distances.get(w).get(u);
                    double auxDist = 2.0;
                    double counterIncr = 0.0;
                    if (Double.isFinite(distWU))
                    {
                        auxDist = Math.min(distWU, auxDist);
                        if (auxDist != distWU)
                        {
                            auxCounter.put(distWU, auxCounter.get(distWU) - 2L);
                        }
                        if (auxCounter.get(distWU) == 0)
                        {
                            auxCounter.remove(distWU);
                        }
                        auxDist -= distWU;
                    }
                    else
                    {
                        counterIncr = 1.0;
                    }

                    return new Pair<>(auxDist, counterIncr);
                }).reduce(new Pair<>(0.0, 0.0), (x, y) -> new Pair<>(x.v1() + y.v1(), x.v2() + y.v2()));
                auxSum += 2 * pair.v1();
                auxCount += 2 * pair.v2();

                double top = counter.lastKey();
                Double current = 3.0;
                while ((current = counter.ceilingKey(current + 1.0)) != null)
                {
                    if (!auxCounter.containsKey(current))
                    {
                        break;
                    }

                    double distWW = current;
                    auxSum += pairsAtDistance.get(current).stream().mapToDouble(p ->
                    {
                        U w1 = p.v1();
                        U w2 = p.v2();

                        if (!w1.equals(u) && !w2.equals(v))
                        {
                            if (uCIn.contains(w1) && vCOut.contains(w2))
                            {
                                double auxDist = Math.min(distances.get(w1).get(u) + 1.0 + distances.get(v).get(w2), distWW);
                                return auxDist - distWW;
                            }
                            return 0.0;
                        }
                        else if (!w1.equals(u) && w2.equals(v))
                        {
                            if (uCIn.contains(w1) && distances.get(w1).get(u) > 1)
                            {
                                double auxDist = Math.min(distances.get(w1).get(u) + 1.0, distWW);
                                return auxDist - distWW;
                            }
                            return 0.0;
                        }
                        else if (w1.equals(u) && !w2.equals(v))
                        {
                            if (vCOut.contains(w2) && distances.get(v).get(w2) > 1)
                            {
                                double auxDist = Math.min(1.0 + distances.get(v).get(w2), distWW);
                                return auxDist - distWW;
                            }
                            return 0.0;
                        }
                        return 0.0;
                    }).sum();
                }
            }
        }
        // Case 2: Node v is isolated.
        else if (!uCIn.isEmpty())
        {
            for (U w : uCIn)
            {
                double distWU = distances.get(w).get(u);
                if (w != u)
                {
                    auxSum += 2 * (distWU + 1.0);
                    auxCount += 2.0;
                }
            }
        }
        // Case 3: Node u is isolated.
        else if (!vCOut.isEmpty())
        {
            for (U w : vCOut)
            {
                double distWV = distances.get(w).get(v);
                if (w != v)
                {
                    auxSum += 2 * (distWV + 1.0);
                    auxCount += 2.0;
                }
            }
        }


        return oldasl - auxSum / auxCount;
    }


}
