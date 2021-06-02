/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.metrics.vertex;

import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;
import es.uam.eps.ir.sonalire.graph.multigraph.MultiGraph;
import es.uam.eps.ir.sonalire.metrics.VertexMetric;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation.MUTUAL;
import static es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation.UND;

/**
 * Computes the coreness (or core number) of the nodes. A k-core of a graph is the maximal
 * subgraph such that each vertex in the subgraph has, at least, degree k. The coreness of a
 * node is k if and only if it belongs to the k-core, but not to the (k+1)-core.
 *
 * @param <U> type of the users.
 *
 * <p>
 * <b>References: </b>
 *   <ol>
 *     <li>Seidman, S.B. Network structure and minimum degree. Social Networks 5(3), pp. 269-287 (1983)</li>
 *     <li>Batagelj, V., Zaversnik. An O(m) Algorithm for Cores Decomposition of networks. arXiv (2003) </li>
 *   </ol>
 * </p>
 */
public class Coreness<U> implements VertexMetric<U>
{
    /**
     * The orientation to choose for the edges.
     */
    private final EdgeOrientation orient;

    /**
     * Constructor.
     * @param orient the orientation to choose for the edges.
     */
    public Coreness(EdgeOrientation orient)
    {
        this.orient = orient;
    }

    @Override
    public double compute(Graph<U> graph, U user)
    {
        return this.compute(graph).get(user);
    }

    @Override
    public Map<U, Double> compute(Graph<U> graph, Stream<U> users)
    {
        Map<U, Double> full = this.compute(graph);

        Map<U, Double> res = new ConcurrentHashMap<>();
        users.filter(graph::containsVertex).forEach(x -> res.put(x, full.get(x)));
        return res;
    }

    @Override
    public Map<U, Double> compute(Graph<U> graph)
    {
        Map<U, Double> coreness = new HashMap<>();

        // We first identify the graph degree values:
        Object2IntMap<U> degreeTable = new Object2IntOpenHashMap<>();
        TreeMap<Integer, List<U>> sortedMap = new TreeMap<>();

        graph.getAllNodes().forEach(node ->
        {
            int degree = graph.degree(node, orient);
            degreeTable.put(node, degree);
            if(!sortedMap.containsKey(degree))
            {
                sortedMap.put(degree, new ArrayList<>());
            }
            sortedMap.get(degree).add(node);
        });

        // We initialize a list of the already visited users.
        List<U> visited = new ArrayList<>();

        Map.Entry<Integer, List<U>> entry = sortedMap.firstEntry();
        List<U> currentDegreeList;

        while(!sortedMap.isEmpty())
        {
            int degreeU = entry.getKey();

            currentDegreeList = entry.getValue();
            U u = currentDegreeList.get(0);
            currentDegreeList.remove(0);
            coreness.put(u, degreeU + 0.0);
            visited.add(u);

            // Now, run over the different neighbors:

            graph.getNeighbourhood(u, orient.invertSelection()).filter(v -> !visited.contains(v)).forEach(v ->
            {
                int degreeV = degreeTable.getInt(v);
                if(degreeV > degreeU) // Decrease the coreness of the neighbor node:
                {
                    int numEdges;

                    if(graph.isMultigraph())
                    {
                        MultiGraph<U> multiGraph = (MultiGraph<U>) graph;
                        if(graph.isDirected())
                        {
                            numEdges = switch (orient.invertSelection())
                            {
                                case IN -> multiGraph.getNumEdges(v, u);
                                case OUT -> multiGraph.getNumEdges(u, v);
                                default -> multiGraph.getNumEdges(u,v) + multiGraph.getNumEdges(v,u);
                            };
                        }
                        else
                        {
                            numEdges = multiGraph.getNumEdges(v,u);
                        }
                    }
                    else if(graph.isDirected() && (orient == UND || orient == MUTUAL))
                    {
                        numEdges = (graph.containsEdge(u,v) ? 1 : 0) + (graph.containsEdge(v,u) ? 1 : 0);
                    }
                    else
                    {
                        numEdges = 1;
                    }

                    degreeTable.put(v, degreeV - numEdges);
                    sortedMap.get(degreeV).remove(v);
                    if(sortedMap.get(degreeV).isEmpty())
                    {
                        sortedMap.remove(degreeV);
                    }
                    if(!sortedMap.containsKey(degreeV - numEdges))
                    {
                        sortedMap.put(degreeV - numEdges, new ArrayList<>());
                    }
                    sortedMap.get(degreeV - numEdges).add(v);
                }
            });

            // If the degree we are now visiting is empty, then, we continue with the next degree
            if(currentDegreeList.isEmpty())
            {
                sortedMap.remove(degreeU);
                if(!sortedMap.isEmpty())
                {
                    entry = sortedMap.firstEntry();
                }
            }
        }

        return coreness;
    }
}