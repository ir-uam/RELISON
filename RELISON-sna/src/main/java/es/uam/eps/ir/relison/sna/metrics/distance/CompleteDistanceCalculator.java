/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.sna.metrics.distance;

import com.google.common.util.concurrent.AtomicDouble;
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.sna.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.relison.sna.community.detection.connectedness.StronglyConnectedComponents;
import es.uam.eps.ir.relison.graph.DirectedGraph;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.generator.EmptyGraphGenerator;
import es.uam.eps.ir.relison.graph.generator.GraphGenerator;
import es.uam.eps.ir.relison.graph.generator.exception.GeneratorBadConfiguredException;
import es.uam.eps.ir.relison.graph.generator.exception.GeneratorNotConfiguredException;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Computes some of the distance based metrics: distances, number of geodesic paths between two nodes, betweenness.
 *
 * <p>
 * <b>References: </b></p>
 *     <ol>
 *         <li>M.E.J. Newman. Networks: an introduction (2010)</li>
 *         <li>M.E.J. Newman, M. Girvan. Finding and Evaluating Community Structure in Networks. Physical Review E 69(2): 026113 (2004)</li>
 *     </ol>
 *
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class CompleteDistanceCalculator<U> implements DistanceCalculator<U>
{
    /**
     * Graph the betweenness metrics are built for.
     */
    private Graph<U> graph;
    /**
     * Nodes betweenness map.
     */
    private Map<U, Double> nodeBetweenness;
    /**
     * Edge betweenness map.
     */
    private Map<U, Map<U, Double>> edgeBetweenness;
    /**
     * Distances map from u to v.
     */
    private Map<U, Map<U, Double>> distancesFrom;
    /**
     * Distances map towards the key user.
     */
    private Map<U, Map<U, Double>> distancesTo;

    /**
     * Number of minimum distance paths between two nodes.
     */
    private Map<U, Map<U, Double>> geodesics;
    /**
     * Strongly connected components.
     */
    private Communities<U> scc;
    /**
     * Flag for detecting whether the algorithm should or should not stop.
     */
    private boolean flag = true;

    /**
     * Average shortest path length.
     */
    private double asl;
    /**
     * Number of pairs at infinite distance to each other.
     */
    private double infiniteDistances;
    /**
     * Constructor.
     */
    public CompleteDistanceCalculator()
    {
        this.graph = null;
    }

    @Override
    public boolean computeDistances(Graph<U> graph)
    {
        if (this.graph != null && this.graph.equals(graph))
        {
            return true;
        }

        CommunityDetectionAlgorithm<U> sccAlg = new StronglyConnectedComponents<>();
        this.scc = sccAlg.detectCommunities(graph);
        // Initialize the values for node betweenness, edge betweenness and distances
        nodeBetweenness = new ConcurrentHashMap<>();
        edgeBetweenness = new ConcurrentHashMap<>();
        distancesFrom = new ConcurrentHashMap<>();
        distancesTo = new ConcurrentHashMap<>();
        geodesics = new ConcurrentHashMap<>();

        boolean weighted = graph.isWeighted();

        // Configure an empty graph generator.
        GraphGenerator<U> gf = new EmptyGraphGenerator<>();
        gf.configure(true, weighted);

        // First, for all nodes, we initialize the node betweenness to zero
        graph.getAllNodes().forEach(node -> this.nodeBetweenness.put(node, 0.0));
        // Do the same for edge betweenness.
        graph.getAllNodes().forEach(node ->
        {
            this.edgeBetweenness.put(node, new HashMap<>());
            graph.getAdjacentNodes(node).forEach(adj -> this.edgeBetweenness.get(node).put(adj, 0.0));
        });

        // Initialize all distances to Infinity.
        graph.getAllNodes().forEach(orig ->
        {
            Object2DoubleMap<U> distFrom = new Object2DoubleOpenHashMap<>();
            Object2DoubleMap<U> distTo = new Object2DoubleOpenHashMap<>();
            Object2DoubleMap<U> geodesics = new Object2DoubleOpenHashMap<>();

            distFrom.defaultReturnValue(Double.POSITIVE_INFINITY);
            distTo.defaultReturnValue(Double.POSITIVE_INFINITY);
            geodesics.defaultReturnValue(0.0);
            this.distancesFrom.put(orig, distFrom);
            this.distancesTo.put(orig, distTo);
            this.geodesics.put(orig, geodesics);
        });

        AtomicInteger atom = new AtomicInteger();
        atom.set(0);

        // Then, for each user u, apply BFS to obtain the distances to other nodes.
        // We compute this in parallel to obtain much faster results.
        Pair<Double> pair = graph.getAllNodes().map(u ->
        {
            double asl = 0.0;
            double counter = 0.0;
            // We want to create a tree.
            DirectedGraph<U> tree;
            try
            {
                Set<U> visited = new HashSet<>();

                tree = (DirectedGraph<U>) gf.generate();

                tree.addNode(u);

                // STEP 1: Compute the weights and distances.
                AtomicDouble d = new AtomicDouble();
                d.set(0.0);

                // Weights (number of paths between the nodes and the source node, u)
                Map<U, Double> weights = new HashMap<>();
                weights.put(u, 1.0);

                // Distances (distances between the nodes and the source node, u)
                Map<U, Double> dist = new HashMap<>();
                dist.put(u, d.get());

                Queue<U> queue = new LinkedList<>();
                Queue<U> nextLevelQueue = new LinkedList<>();

                Map<Double, Set<U>> levels = new HashMap<>();
                levels.put(d.get(), new HashSet<>());
                levels.get(d.get()).add(u);
                levels.put(d.get() + 1.0, new HashSet<>());

                // STEP 1: BFS. Find the distances between nodes.
                queue.add(u);
                visited.add(u);
                while (!queue.isEmpty())
                {
                    U current = queue.poll();
                    if(!visited.contains(current))
                    {
                        asl = asl + (d.get() - asl)/(counter+1.0);
                        ++counter;
                        visited.add(current);
                    }

                    graph.getAdjacentNodes(current).forEach(node ->
                    {
                        if (!dist.containsKey(node))
                        {
                            dist.put(node, d.get() + 1.0);
                            weights.put(node, weights.get(current));
                            nextLevelQueue.add(node);
                            levels.get(d.get() + 1.0).add(node);
                            tree.addNode(node);
                            tree.addEdge(node, current);
                        }
                        else if (dist.get(node).equals(d.get() + 1.0))
                        {
                            weights.put(node, weights.get(current) + weights.get(node));
                            tree.addEdge(node, current);
                        }
                        // else { do nothing }
                    });

                    if (queue.isEmpty())
                    {
                        while (!nextLevelQueue.isEmpty())
                        {
                            queue.add(nextLevelQueue.poll());
                        }
                        levels.put(d.addAndGet(1.0) + 1.0, new HashSet<>());
                    }
                }

                // STEP 2: Compute the node and edge betweenness
                double level = d.get() - 1.0;

                Map<U, Double> nodeBetw = new HashMap<>();
                Map<U, Map<U, Double>> edgeBetw = new HashMap<>();
                Map<U, Double> accumulated = new HashMap<>();
                while (level >= 0.0)
                {
                    Set<U> ithLevel = levels.get(level);
                        ithLevel.forEach(node ->
                    {
                        if (tree.inDegree(node) == 0) // Leaf
                        {
                            nodeBetw.put(node, 0.0);
                            edgeBetw.put(node, new HashMap<>());
                            tree.getAdjacentNodes(node).forEach(adj ->
                            {
                                double value = weights.get(adj) / weights.get(node);
                                edgeBetw.get(node).put(adj, value);
                                if (!accumulated.containsKey(adj))
                                {
                                    accumulated.put(adj, 0.0);
                                }
                                accumulated.put(adj, accumulated.get(adj) + value);
                            });
                        }
                        else // Not leaf
                        {
                            double score = tree.getIncidentNodes(node).mapToDouble(incid ->
                            {
                                double nodeb = nodeBetw.get(incid);
                                double weightA = weights.get(node);
                                double weightB = weights.get(incid);
                                return (1 + nodeb) * weightA / weightB;
                            }).sum();
                            nodeBetw.put(node, score);
                            edgeBetw.put(node, new HashMap<>());
                            tree.getAdjacentNodes(node).forEach(adj ->
                            {
                                double value = weights.get(adj) / weights.get(node);
                                value = value * (1.0 + accumulated.get(node));
                                edgeBetw.get(node).put(adj, value);
                                if (!accumulated.containsKey(adj))
                                {
                                    accumulated.put(adj, 0.0);
                                }
                                accumulated.put(adj, accumulated.get(adj) + value);
                            });
                        }
                    });
                    level--;
                }

                // Update the distance map and the number of geodesic paths between nodes
                dist.forEach((v, value) ->
                {
                    double val = value;
                    this.distancesFrom.get(u).put(v, val);
                    this.distancesTo.get(v).put(u, val);
                    this.geodesics.get(u).put(v, weights.get(v));
                });

                // Update the node betweenness map
                nodeBetw.forEach((v, value) ->
                {
                    if (!v.equals(u))
                    {
                        this.nodeBetweenness.put(v, this.nodeBetweenness.get(v) + value);
                    }
                });

                // Update the edge betweenness map
                edgeBetw.forEach((v, value) -> value.forEach((w, value1) -> this.edgeBetweenness.get(w).put(v, this.edgeBetweenness.get(w).get(v) + value1)));
            }
            catch (GeneratorNotConfiguredException | GeneratorBadConfiguredException ex)
            {
                flag = false;
            }


            int count = atom.incrementAndGet();
            if(count % 1000 == 0)
            {
                System.err.println("Run over " + count + " users." );
            }

            return new Pair<>(asl, counter);

        }).reduce(new Pair<>(0.0,0.0), (x,y) ->
        {
            double total = x.v2() + y.v2();
            if(total == 0) return new Pair<>(0.0,0.0);
            double asl = (x.v2())/total * x.v1() + (y.v2())/total * y.v1();
            return new Pair<>(asl, total);
        });

        this.asl = pair.v1();
        this.infiniteDistances = graph.getVertexCount()*(graph.getVertexCount()-1.0)-pair.v2();

        if (!flag)
        {
            return false;
        }

        this.graph = graph;
        return true;
    }

    @Override
    public Map<U, Double> getNodeBetweenness()
    {
        return this.nodeBetweenness;
    }

    @Override
    public double getNodeBetweenness(U node)
    {
        return this.nodeBetweenness.get(node);
    }

    @Override
    public Map<U, Map<U, Double>> getEdgeBetweenness()
    {
        return this.edgeBetweenness;
    }

    @Override
    public Map<U, Double> getEdgeBetweenness(U node)
    {
        if (this.edgeBetweenness.containsKey(node))
        {
            return this.edgeBetweenness.get(node);
        }
        return new HashMap<>();
    }

    @Override
    public double getEdgeBetweenness(U orig, U dest)
    {
        if (this.edgeBetweenness.containsKey(orig))
        {
            if (this.edgeBetweenness.get(orig).containsKey(dest))
            {
                return this.edgeBetweenness.get(orig).get(dest);
            }
        }
        return -1.0;
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
        return this.geodesics;
    }

    @Override
    public Map<U, Double> getGeodesics(U node)
    {
        if (this.geodesics.containsKey(node))
        {
            return this.geodesics.get(node);
        }
        return new HashMap<>();
    }

    @Override
    public double getGeodesics(U orig, U dest)
    {
        if (this.geodesics.containsKey(orig))
        {
            if (this.geodesics.get(orig).containsKey(dest))
            {
                return this.geodesics.get(orig).get(dest);
            }
        }
        return 0.0;
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
        return this.infiniteDistances;
    }
}
