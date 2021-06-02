/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.metrics.distance;

import es.uam.eps.ir.sonalire.community.Communities;
import es.uam.eps.ir.sonalire.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.sonalire.community.detection.connectedness.StronglyConnectedComponents;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.graph.generator.EmptyGraphGenerator;
import es.uam.eps.ir.sonalire.graph.generator.GraphGenerator;
import es.uam.eps.ir.sonalire.utils.datatypes.Pair;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fast version of a distance calculator which just computes the distances between pairs of nodes.
 * Unsupported metrics: node betweenness, edge betweenness, geodesics.
 *
 * <p>
 * <b>References: </b> M.E.J. Newman. Networks: an introduction (2010)
 * </p>
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class FastDistanceCalculator<U> implements DistanceCalculator<U>
{
    /**
     * Graph for which we compute the distances.
     */
    private Graph<U> graph;
    /**
     * Distances map from u to v.
     */
    private Map<U, Map<U, Double>> distancesFrom;
    /**
     * Distances map towards the key user.
     */
    private Map<U, Map<U, Double>> distancesTo;
    /**
     * Strongly connected components of the network.
     */
    private Communities<U> scc;
    /**
     * The average shortest path length.
     */
    private double asl;
    /**
     * The number of pairs of users at infinite distance from each other.
     */
    private double infiniteDist;

    /**
     * Constructor
     */
    public FastDistanceCalculator()
    {
        this.graph = null;
    }

    /**
     * Computes the betweenness of a graph.
     *
     * @param graph the graph.
     *
     * @return true if everything went ok.
     */
    public boolean computeDistances(Graph<U> graph)
    {
        if (this.graph != null && this.graph.equals(graph))
        {
            return true;
        }

        this.graph = null;

        // First, we find the strongly connected components of the network.
        CommunityDetectionAlgorithm<U> sccAlg = new StronglyConnectedComponents<>();
        this.scc = sccAlg.detectCommunities(graph);

        // Initialize the distance maps:
        distancesFrom = new ConcurrentHashMap<>();
        distancesTo = new ConcurrentHashMap<>();

        boolean weighted = graph.isWeighted();

        // Configure an empty graph generator.
        GraphGenerator<U> gf = new EmptyGraphGenerator<>();
        gf.configure(true, weighted);

        // Create the different distance maps.
        graph.getAllNodes().forEach(node ->
        {
            Object2DoubleMap<U> distFrom = new Object2DoubleOpenHashMap<>();
            Object2DoubleMap<U> distTo = new Object2DoubleOpenHashMap<>();
            distFrom.defaultReturnValue(Double.POSITIVE_INFINITY);
            distTo.defaultReturnValue(Double.POSITIVE_INFINITY);
            this.distancesFrom.put(node, distFrom);
            this.distancesTo.put(node, distTo);
        });


        AtomicInteger atom = new AtomicInteger();
        atom.set(0);
        // Compute the distances from each node to the rest.
        Pair<Double> allAsl = graph.getAllNodes().parallel().map(u ->
        {
            Map<U, Double> distFrom = distancesFrom.get(u);

            double currentDist = 0.0;

            double asl = 0.0;
            double current = 0.0;

            Set<U> visited = new HashSet<>();
            Queue<U> queue = new LinkedList<>();
            Queue<U> nextLevelQueue = new LinkedList<>();
            queue.add(u);

            while (!queue.isEmpty())
            {
                U v = queue.poll();
                if (!visited.contains(v))
                {
                    visited.add(v);
                    distFrom.put(v, currentDist);
                    synchronized (this)
                    {
                        distancesTo.get(v).put(u, currentDist);
                    }
                    graph.getAdjacentNodes(v).forEach(nextLevelQueue::add);

                    asl = asl + (currentDist - asl)/(current + 1.0);
                    ++current;
                }

                if (queue.isEmpty())
                {
                    queue = nextLevelQueue;
                    nextLevelQueue = new LinkedList<>();
                    ++currentDist;
                }
            }

            int count = atom.incrementAndGet();
            if(count % 1000 == 0)
            {
                System.err.println("Run over " + count + " users." );
            }

            return new Pair<>(asl, current);
        }).reduce(new Pair<>(0.0,0.0), (x,y) ->
        {
            double total = x.v2() + y.v2();
            if(total == 0) return new Pair<>(0.0,0.0);
            double asl = (x.v2()/total)*x.v1() + (y.v2()/total)*y.v1();
            return new Pair<>(asl, total);
        });

        this.asl = allAsl.v1();
        this.infiniteDist = graph.getVertexCount()*(graph.getVertexCount()-1.0) - allAsl.v2();

        this.graph = graph;
        return true;
    }

    @Override
    public Map<U, Double> getNodeBetweenness()
    {
        throw new UnsupportedOperationException("Unsupported method");
    }

    @Override
    public double getNodeBetweenness(U node)
    {
        throw new UnsupportedOperationException("Unsupported method");
    }

    @Override
    public Map<U, Map<U, Double>> getEdgeBetweenness()
    {
        throw new UnsupportedOperationException("Unsupported method");
    }

    @Override
    public Map<U, Double> getEdgeBetweenness(U node)
    {
        throw new UnsupportedOperationException("Unsupported method");
    }

    @Override
    public double getEdgeBetweenness(U orig, U dest)
    {
        throw new UnsupportedOperationException("Unsupported method");
    }

    @Override
    public Map<U, Map<U, Double>> getDistances()
    {
        return this.distancesFrom;
    }

    @Override
    public Map<U, Double> getDistancesFrom(U node)
    {
        if (this.distancesFrom.containsKey(node))
        {
            return this.distancesFrom.get(node);
        }
        return new HashMap<>();
    }

    @Override
    public Map<U, Double> getDistancesTo(U node)
    {
        if (this.distancesTo.containsKey(node))
        {
            return this.distancesTo.get(node);
        }
        return new HashMap<>();
    }

    @Override
    public double getDistances(U orig, U dest)
    {
        if (this.distancesFrom.containsKey(orig))
        {
            if (this.distancesFrom.get(orig).containsKey(dest))
            {
                return this.distancesFrom.get(orig).get(dest);
            }
        }
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public Map<U, Map<U, Double>> getGeodesics()
    {
        throw new UnsupportedOperationException("Unsupported method");
    }

    @Override
    public Map<U, Double> getGeodesics(U node)
    {
        throw new UnsupportedOperationException("Unsupported method");
    }

    @Override
    public double getGeodesics(U orig, U dest)
    {
        throw new UnsupportedOperationException("Unsupported method");
    }

    @Override
    public Communities<U> getSCC()
    {
        return this.scc;
    }

    @Override
    public double getASL()
    {
        return this.asl;
    }

    @Override
    public double getInfiniteDistances()
    {
        return this.infiniteDist;
    }
}
