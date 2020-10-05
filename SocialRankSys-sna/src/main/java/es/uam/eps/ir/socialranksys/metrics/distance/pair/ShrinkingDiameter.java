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
 * Computes the variation of the diameter if a link is
 * included in the graph.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class ShrinkingDiameter<U> extends AbstractPairMetric<U>
{
    /**
     * Distance calculator.
     */
    private final DistanceCalculator<U> dc;

    /**
     * Constructor
     */
    public ShrinkingDiameter()
    {
        dc = new FastDistanceCalculator<>();
    }

    /**
     * Constructor.
     *
     * @param dc distance calculator.
     */
    public ShrinkingDiameter(DistanceCalculator<U> dc)
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

        distances.keySet().forEach((u) -> distances.get(u).keySet().forEach((v) ->
        {
            double distance = distances.get(u).get(v);
            if (Double.isFinite(distance))
            {
                if (!pairsAtDistance.containsKey(distance))
                {
                    pairsAtDistance.put(distance, new ArrayList<>());
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
                cins.get(v).add(u);
                couts.get(u).add(v);
            }
        })
        );

        Set<U> uCIn = cins.getOrDefault(orig, new HashSet<>());
        Set<U> vCIn = cins.getOrDefault(dest, new HashSet<>());
        Set<U> uCOut = couts.getOrDefault(orig, new HashSet<>());
        Set<U> vCOut = couts.getOrDefault(dest, new HashSet<>());

        return this.computePair(graph, orig, dest, uCIn, uCOut, vCIn, vCOut, pairsAtDistance, distances);
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

        distances.keySet().forEach((u) -> distances.get(u).keySet().forEach((v) ->
        {
            double distance = distances.get(u).get(v);
            if (Double.isFinite(distance))
            {
                if (!pairsAtDistance.containsKey(distance))
                {
                    pairsAtDistance.put(distance, new ArrayList<>());
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
                 cins.get(v).add(u);
                 couts.get(u).add(v);
             }
        })
        );

        Map<Pair<U>, Double> values = new HashMap<>();
        // Then, for each pair of users in the network, find the value of the metric
        graph.getAllNodes().forEach(u ->
        {
            Set<U> uCIn = cins.getOrDefault(u, new HashSet<>());
            Set<U> uCOut = couts.getOrDefault(u, new HashSet<>());
            graph.getAllNodes().forEach(v ->
            {
                Set<U> vCIn = cins.getOrDefault(v, new HashSet<>());
                Set<U> vCOut = couts.getOrDefault(v, new HashSet<>());
                double value = computePair(graph, u, v, uCIn, uCOut, vCIn, vCOut, pairsAtDistance, distances);
                values.put(new Pair<>(u, v), value);
            });
        });

        return values;
    }

    /**
     * Computes the metric for an individual pair of users.
     *
     * @param graph           the graph.
     * @param u               the origin user.
     * @param v               the destination user.
     * @param uCIn            the incoming component of user u
     * @param uCOut           the outgoing component of user u
     * @param vCIn            the incoming component of user v
     * @param vCOut           the outgoing component of user v
     * @param pairsAtDistance map that contains the different pairs of users at a given distance.
     * @param distances       the distances.
     *
     * @return the value of the metric.
     */
    private double computePair(Graph<U> graph, U u, U v, Set<U> uCIn, Set<U> uCOut, Set<U> vCIn, Set<U> vCOut, TreeMap<Double, List<Pair<U>>> pairsAtDistance, Map<U, Map<U, Double>> distances)
    {
        // If the edge already exists, nothing is modified.
        if (graph.containsEdge(u, v))
        {
            return 0.0;
        }

        // Start with users with infinite distances.
        TreeSet<Double> auxDistances = new TreeSet<>();
        double diam = pairsAtDistance.lastKey();
        double auxdiam = 0.0;

        // If the distance between u and v is finite, no infinite distances will be updated.
        if (!Double.isFinite(distances.get(u).get(v)))
        {
            Set<U> aux = new HashSet<>(uCIn);
            aux.removeAll(vCIn);
            for (U w : aux)
            {
                double dist = distances.get(w).get(u) + 1.0;
                auxDistances.add(dist);
                for (U x : vCOut)
                {
                    dist = Math.min(distances.get(w).get(x), dist + distances.get(v).get(w));
                    auxDistances.add(dist);
                }
            }

            aux = new HashSet<>(vCOut);
            aux.removeAll(uCOut);
            for (U w : aux)
            {
                double dist = distances.get(w).get(u) + 1.0;
                auxDistances.add(dist);
                for (U x : vCOut)
                {
                    dist = Math.min(distances.get(w).get(x), dist + distances.get(w).get(u));
                    auxDistances.add(dist);
                }
            }

            if (!auxDistances.isEmpty()) // No items at infinite distance:
            {
                auxdiam = auxDistances.last(); // Then, the diameter does not increase -> it can only be reduced.
            }
            if (auxdiam >= diam)
            {
                return diam - auxdiam; // else: for this part, this is not increasing
            }
        }

        // Finite distances.
        for (double key : pairsAtDistance.descendingKeySet())
        {
            // If no value is in auxDistances, use the previous one.
            if (!auxDistances.isEmpty())
            {
                auxdiam = auxDistances.last();
            }

            // If this happens, we had already found the current diameter.
            if (key < auxdiam)
            {
                return diam - auxdiam;
            }

            // We check the different pairs at a given distance.
            for (Pair<U> pairs : pairsAtDistance.get(key))
            {
                U w1 = pairs.v1();
                U w2 = pairs.v2();

                if (graph.isDirected()) // First case: the graph is directed.
                {
                    // If the origin node is v, then, we do not win anything
                    // Same if the destination node is u
                    if (w2.equals(u) || w1.equals(v))
                    {
                        return diam - key;
                    }
                    else if (w1.equals(u) && w2.equals(v))
                    {
                        auxDistances.add(1.0); // if the pair corresponds to the edge we want to add: distance == 1.0
                    }
                    else if (w1.equals(u)) // Origin node: u
                    {
                        // If we cannot reach the node from v, it is the same.
                        if (!vCOut.contains(w2))
                        {
                            return diam - key;
                        }
                        else // Update:
                        {
                            double dist = Math.min(key, 1 + distances.get(v).get(w2));
                            // If this happens: the diameter does not reduce -> therefore, we found it.
                            if (dist == key)
                            {
                                return diam - key;
                            }
                            else
                            {
                                auxDistances.add(dist);
                            }
                        }
                    }
                    else if (w2.equals(v))  // Destination node: v
                    {
                        // If we cannot reach node u from w1, we cannot obtain an advantage 
                        // from using that node.
                        if (!uCIn.contains(w1))
                        {
                            return diam - key;
                        }
                        else // Update:
                        {
                            double dist = Math.min(key, 1 + distances.get(w1).get(u));
                            // If this happens: the diameter does not reduce -> therefore, we found it.
                            if (dist == key)
                            {
                                return diam - key;
                            }
                            else
                            {
                                auxDistances.add(dist);
                            }
                        }
                    }
                    else // !w1.equals(u) && !w2.equals(v)
                    {
                        // We need to be able to go from w1 to w2 via (u,v) edge.
                        // Otherwise, the diameter cannot be updated.
                        if (!uCIn.contains(w1) || !vCOut.contains(w2))
                        {
                            return diam - key;
                        }
                        else
                        {
                            double dist = Math.min(key, distances.get(w1).get(u) + distances.get(v).get(w2) + 1.0);
                            // If this happens: the diameter does not reduce -> therefore, we found it.
                            if (dist == key)
                            {
                                return diam - key;
                            }
                            else
                            {
                                auxDistances.add(dist);
                            }
                        }
                    }
                }
                else // if is undirected
                {
                    // In this case, (u,v) or (v,u) update its distance to 1
                    if ((w1.equals(u) && w2.equals(v)) || (w2.equals(u) && w1.equals(v)))
                    {
                        auxDistances.add(1.0);
                    }
                    else if (w1.equals(u))
                    {
                        // If we cannot reach w2 from v...
                        if (!vCOut.contains(w2))
                        {
                            return diam - key;
                        }
                        else
                        {
                            double dist = Math.min(key, 1 + distances.get(v).get(w2));
                            if (dist == key)
                            {
                                return diam - key;
                            }
                            else
                            {
                                auxDistances.add(dist);
                            }
                        }
                    }
                    else if (w2.equals(u))
                    {
                        // If we cannot reach w1 from v...
                        if (!vCOut.contains(w1))
                        {
                            return diam - key;
                        }
                        else
                        {
                            double dist = Math.min(key, 1 + distances.get(v).get(w1));
                            if (dist == key)
                            {
                                return diam - key;
                            }
                            else
                            {
                                auxDistances.add(dist);
                            }
                        }
                    }
                    else if (w1.equals(v))
                    {
                        // If we cannot reach u from w2...
                        if (!uCIn.contains(w2))
                        {
                            return diam - key;
                        }
                        else
                        {
                            double dist = Math.min(key, 1 + distances.get(w2).get(u));
                            if (dist == key)
                            {
                                return diam - key;
                            }
                            else
                            {
                                auxDistances.add(dist);
                            }
                        }
                    }
                    else if (w2.equals(v))
                    {
                        // If we cannot reach u from w1...
                        if (!uCIn.contains(w1))
                        {
                            return diam - key;
                        }
                        else
                        {
                            double dist = Math.min(key, 1 + distances.get(w1).get(u));
                            if (dist == key)
                            {
                                return diam - key;
                            }
                            else
                            {
                                auxDistances.add(dist);
                            }
                        }
                    }
                    else // if w1 != u,v and w2 != u,v
                    {
                        // from w1 to w2 via (u,v)
                        if (uCIn.contains(w1) && vCOut.contains(w2))
                        {
                            double dist = Math.min(key, distances.get(w1).get(u) + distances.get(v).get(w2) + 1.0);
                            if (dist == key)
                            {
                                return diam - key;
                            }
                            else
                            {
                                auxDistances.add(dist);
                            }
                        }
                        else if (vCOut.contains(w1) && uCIn.contains(w2)) // from w1 to w2 via (v,u)
                        {
                            double dist = Math.min(key, distances.get(w2).get(u) + distances.get(v).get(w1) + 1.0);
                            if (dist == key)
                            {
                                return diam - key;
                            }
                            else
                            {
                                auxDistances.add(dist);
                            }
                        }
                        else
                        {
                            return diam - key;
                        }
                    }
                }
            }
        }

        auxdiam = auxDistances.last();
        return diam - auxdiam;
    }


    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph, Stream<Pair<U>> pairs)
    {
        // First, we find the pairs of users at maximum distance (diameter)
        this.dc.computeDistances(graph);

        Map<U, Map<U, Double>> distances = this.dc.getDistances();
        Map<U, Set<U>> cins = new HashMap<>();
        Map<U, Set<U>> couts = new HashMap<>();
        TreeMap<Double, List<Pair<U>>> pairsAtDistance = new TreeMap<>();

        distances.keySet().forEach((u) -> distances.get(u).keySet().forEach((v) ->
        {
            double distance = distances.get(u).get(v);
            if (Double.isFinite(distance))
            {
                if (!pairsAtDistance.containsKey(distance))
                {
                    pairsAtDistance.put(distance, new ArrayList<>());
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
                cins.get(v).add(u);
                couts.get(u).add(v);
            }
        })
        );

        Map<Pair<U>, Double> values = new ConcurrentHashMap<>();
        pairs.forEach(pair ->
        {
            U u = pair.v1();
            U v = pair.v2();
            Set<U> uCIn = cins.getOrDefault(u, new HashSet<>());
            Set<U> vCIn = cins.getOrDefault(v, new HashSet<>());
            Set<U> uCOut = couts.getOrDefault(u, new HashSet<>());
            Set<U> vCOut = couts.getOrDefault(v, new HashSet<>());
            double value = this.computePair(graph, u, v, uCIn, uCOut, vCIn, vCOut, pairsAtDistance, distances);
            values.put(pair, value);
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
        OptionalDouble opt = values.values().stream().mapToDouble(x -> x).average();
        return opt.isPresent() ? opt.getAsDouble() : Double.NaN;
    }
}
