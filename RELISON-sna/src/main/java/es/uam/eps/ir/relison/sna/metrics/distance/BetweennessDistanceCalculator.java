/*
 *  Copyright (C) 2024 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.sna.metrics.distance;

import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.graph.DirectedGraph;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.generator.EmptyGraphGenerator;
import es.uam.eps.ir.relison.graph.generator.GraphGenerator;
import es.uam.eps.ir.relison.graph.generator.exception.GeneratorBadConfiguredException;
import es.uam.eps.ir.relison.graph.generator.exception.GeneratorNotConfiguredException;
import com.google.common.util.concurrent.AtomicDouble;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Computes <b>only</b> the node and edge betweenness of a network, following Brandes' algorithm: each source node is
 * processed independently (a BFS that finds the distances and the number of geodesic paths, then a backwards pass that
 * accumulates the dependencies), and its per-source state is discarded before moving on to the next source.
 *
 * <p>This is the memory-lean counterpart of {@link CompleteDistanceCalculator}: that one performs the very same
 * per-source computation, but additionally stores the distances and the number of geodesic paths <em>between every
 * pair of nodes</em>, so that it can answer distance / geodesic / average-path-length queries afterwards. Those three
 * maps take <b>O(n²)</b> memory, which makes it unusable on large networks for algorithms that only need betweenness
 * (e.g. {@link es.uam.eps.ir.relison.sna.community.detection.modularity.GirvanNewman Girvan-Newman}, which recomputes
 * the edge betweenness once per removed edge). Keeping just the betweenness accumulators brings the memory down to
 * <b>O(n + m)</b>.</p>
 *
 * <p>Consequently, only the betweenness methods are supported: the distance, geodesic, strongly connected component
 * and average shortest path length accessors throw {@link UnsupportedOperationException}. Use
 * {@link CompleteDistanceCalculator} (or {@link FastDistanceCalculator}) when those are needed.</p>
 *
 * <p>
 * <b>References: </b></p>
 *     <ol>
 *         <li>U. Brandes. A faster algorithm for betweenness centrality. Journal of Mathematical Sociology 25(2), pp. 163-177 (2001)</li>
 *         <li>M.E.J. Newman, M. Girvan. Finding and Evaluating Community Structure in Networks. Physical Review E 69(2): 026113 (2004)</li>
 *     </ol>
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class BetweennessDistanceCalculator<U> implements DistanceCalculator<U>
{
    /**
     * Graph the betweenness metrics are built for.
     */
    private Graph<U> graph;
    /**
     * Node betweenness of each node of the network.
     */
    private Map<U, Double> nodeBetweenness;
    /**
     * Edge betweenness of each edge of the network, indexed as (origin, destination).
     */
    private Map<U, Map<U, Double>> edgeBetweenness;
    /**
     * Flag indicating whether the computation succeeded.
     */
    private boolean flag = true;

    @Override
    public boolean computeDistances(Graph<U> graph)
    {
        if (this.graph != null && this.graph.equals(graph))
        {
            return true;
        }

        this.flag = true;
        this.nodeBetweenness = new ConcurrentHashMap<>();
        this.edgeBetweenness = new ConcurrentHashMap<>();

        // Configure an empty graph generator (used to build the per-source shortest path DAG).
        GraphGenerator<U> gf = new EmptyGraphGenerator<>();
        gf.configure(true, graph.isWeighted());

        // Initialize the betweenness accumulators: these are the only structures that survive across sources.
        graph.getAllNodes().forEach(node -> this.nodeBetweenness.put(node, 0.0));
        graph.getAllNodes().forEach(node ->
        {
            this.edgeBetweenness.put(node, new HashMap<>());
            graph.getAdjacentNodes(node).forEach(adj -> this.edgeBetweenness.get(node).put(adj, 0.0));
        });

        graph.getAllNodes().forEach(u ->
        {
            DirectedGraph<U> tree;
            try
            {
                tree = (DirectedGraph<U>) gf.generate();
                tree.addNode(u);

                // STEP 1: BFS from u, computing the distances (dist) and the number of geodesic paths (weights).
                AtomicDouble d = new AtomicDouble();
                d.set(0.0);

                Map<U, Double> weights = new HashMap<>();
                weights.put(u, 1.0);

                Map<U, Double> dist = new HashMap<>();
                dist.put(u, d.get());

                Queue<U> queue = new LinkedList<>();
                Queue<U> nextLevelQueue = new LinkedList<>();

                Map<Double, Set<U>> levels = new HashMap<>();
                levels.put(d.get(), new HashSet<>());
                levels.get(d.get()).add(u);
                levels.put(d.get() + 1.0, new HashSet<>());

                queue.add(u);
                while (!queue.isEmpty())
                {
                    U current = queue.poll();

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
                        // else { the node belongs to a previous level: it is not on a shortest path through current }
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

                // STEP 2: traverse the levels backwards, accumulating the dependencies into node / edge betweenness.
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

                // Accumulate this source's contribution. Unlike CompleteDistanceCalculator, the distances and the
                // number of geodesic paths are NOT stored: dist / weights / levels / tree die with this iteration.
                nodeBetw.forEach((v, value) ->
                {
                    if (!v.equals(u))
                    {
                        this.nodeBetweenness.put(v, this.nodeBetweenness.get(v) + value);
                    }
                });

                edgeBetw.forEach((v, value) -> value.forEach((w, value1) ->
                        this.edgeBetweenness.get(w).put(v, this.edgeBetweenness.get(w).get(v) + value1)));
            }
            catch (GeneratorNotConfiguredException | GeneratorBadConfiguredException ex)
            {
                this.flag = false;
            }
        });

        if (!this.flag)
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
        if (this.edgeBetweenness.containsKey(orig) && this.edgeBetweenness.get(orig).containsKey(dest))
        {
            return this.edgeBetweenness.get(orig).get(dest);
        }
        return -1.0;
    }

    /* ---- Distance-based accessors: not available, since the O(n²) state they need is deliberately not kept. ---- */

    @Override
    public Map<U, Map<U, Double>> getDistances()
    {
        throw new UnsupportedOperationException("This calculator only computes betweenness: use CompleteDistanceCalculator for distances.");
    }

    @Override
    public Map<U, Double> getDistancesFrom(U node)
    {
        throw new UnsupportedOperationException("This calculator only computes betweenness: use CompleteDistanceCalculator for distances.");
    }

    @Override
    public Map<U, Double> getDistancesTo(U node)
    {
        throw new UnsupportedOperationException("This calculator only computes betweenness: use CompleteDistanceCalculator for distances.");
    }

    @Override
    public double getDistances(U orig, U dest)
    {
        throw new UnsupportedOperationException("This calculator only computes betweenness: use CompleteDistanceCalculator for distances.");
    }

    @Override
    public Map<U, Map<U, Double>> getGeodesics()
    {
        throw new UnsupportedOperationException("This calculator only computes betweenness: use CompleteDistanceCalculator for the geodesic paths.");
    }

    @Override
    public Map<U, Double> getGeodesics(U node)
    {
        throw new UnsupportedOperationException("This calculator only computes betweenness: use CompleteDistanceCalculator for the geodesic paths.");
    }

    @Override
    public double getGeodesics(U orig, U dest)
    {
        throw new UnsupportedOperationException("This calculator only computes betweenness: use CompleteDistanceCalculator for the geodesic paths.");
    }

    @Override
    public Communities<U> getSCC()
    {
        throw new UnsupportedOperationException("This calculator only computes betweenness: use CompleteDistanceCalculator for the strongly connected components.");
    }

    @Override
    public double getASL()
    {
        throw new UnsupportedOperationException("This calculator only computes betweenness: use CompleteDistanceCalculator for the average shortest path length.");
    }

    @Override
    public double getInfiniteDistances()
    {
        throw new UnsupportedOperationException("This calculator only computes betweenness: use CompleteDistanceCalculator for the number of infinite distances.");
    }
}
