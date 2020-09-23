/* 
 *  Copyright (C) 2019 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.distance;

import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.socialranksys.community.detection.connectedness.StronglyConnectedComponents;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.generator.EmptyGraphGenerator;
import es.uam.eps.ir.socialranksys.graph.generator.GraphGenerator;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fast version of a distance calculator which just computes the distances between pairs of nodes.
 *
 * Finding and Evaluating Community Structure in Networks. Newman, M.E.J, Girvan, M., Physical Review E 69(2): 026113, February 2004.
 * Networks: An Introduction. Newman, M.E.J., Oxford University Press, 2010.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @param <U> Type of the users
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
    private Map<U,Map<U, Double>> distancesFrom;
    /**
     * Distances map towards the key user.
     */
    private Map<U, Map<U, Double>> distancesTo;
    /**
     * Strongly connected components of the network.
     */
    private Communities<U> scc;

    /**
     * Constructor
     */
    public FastDistanceCalculator()
    {
        this.graph = null;
    }
    
    /**
     * Computes the betweenness of a graph.
     * @param graph the graph.
     * @return true if everything went ok.
     */
    public boolean computeDistances(Graph<U> graph)
    {
        if(this.graph != null && this.graph.equals(graph))
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

        long numNodes = graph.getVertexCount();
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

        // Compute the distances from each node to the rest.
        graph.getAllNodes().parallel().forEach(u ->
        {
            Map<U, Double> distFrom = distancesFrom.get(u);

            double currentDist = 0.0;

            Set<U> visited = new HashSet<>();
            Queue<U> queue = new LinkedList<>();
            Queue<U> nextLevelQueue = new LinkedList<>();
            queue.add(u);

            while(!queue.isEmpty())
            {
                U v = queue.poll();
                if(!visited.contains(v))
                {
                    visited.add(v);
                    distFrom.put(v, currentDist);
                    synchronized (this)
                    {
                        distancesTo.get(v).put(u, currentDist);
                    }
                    graph.getAdjacentNodes(v).forEach(nextLevelQueue::add);
                }

                if(queue.isEmpty())
                {
                    queue = nextLevelQueue;
                    nextLevelQueue = new LinkedList<>();
                    ++currentDist;
                }
            }
        });

        this.graph = graph;
        return true;
    }

    /**
     * Returns the node betweenness for each node in the network.
     * @return a map containing the node betweenness for each node.
     */
    public Map<U, Double> getNodeBetweenness()
    {
        throw new UnsupportedOperationException("Unsupported method");
    }
    
    /**
     * Gets the value of node betweenness for a single node.
     * @param node the value for the node.
     * @return the node betweenness for that node.
     */
    public double getNodeBetweenness(U node)
    {
        throw new UnsupportedOperationException("Unsupported method");
    }
    
    /**
     * Gets all the values of the edge betweenness
     * @return the edge betweenness value for each edge.
     */
    public Map<U, Map<U,Double>> getEdgeBetweenness()
    {
        throw new UnsupportedOperationException("Unsupported method");
    }
    
    /**
     * Returns the edge betweenness of all the adjacent edges to a given node.
     * @param node The node.
     * @return a map containing the values of edge betweenness for all the adjacent links to the given node.
     */
    public Map<U,Double> getEdgeBetweenness(U node)
    {
        throw new UnsupportedOperationException("Unsupported method");
    }
    
    /**
     * Returns the edge betweenness of a single edge.
     * @param orig origin node of the edge.
     * @param dest destination node of the edge.
     * @return the betweenness if the edge exists, -1.0 if not.
     */
    public double getEdgeBetweenness(U orig, U dest)
    {
        throw new UnsupportedOperationException("Unsupported method");
    }
    
    /**
     * Returns all the distances between different pairs.
     * @return the distances between pairs.
     */
    public Map<U, Map<U, Double>> getDistances()
    {
        return this.distancesFrom;
    }
    
    /**
     * Return the distances between a node and the rest of nodes in the network.
     * @param node the node.
     * @return a map containing all the distances from the node to the rest of the network.
     */
    public Map<U, Double> getDistancesFrom(U node)
    {
        if(this.distancesFrom.containsKey(node))
        {
            return this.distancesFrom.get(node);
        }
        return new HashMap<>();
    }
    
    /**
     * Returns the distance between the network and an specific node.
     * @param node the node.
     * @return a map containing all the distances from each vertex in the network to the node.
     */
    public Map<U, Double> getDistancesTo(U node)
    {
        if(this.distancesTo.containsKey(node))
        {
            return this.distancesTo.get(node);
        }
        return new HashMap<>();
    }
    
    /**
     * Returns the distance between two nodes.
     * @param orig origin node.
     * @param dest destination node.
     * @return the distance between both nodes. if there is a path between them, +Infinity if not.
     */
    public double getDistances(U orig, U dest)
    {
        if(this.distancesFrom.containsKey(orig))
            if(this.distancesFrom.get(orig).containsKey(dest))
                return this.distancesFrom.get(orig).get(dest);
        return Double.POSITIVE_INFINITY;
    }
    
    /**
     * Returns the number of geodesic paths between different pairs.
     * @return the distances between pairs.
     */
    public Map<U, Map<U, Double>> getGeodesics()
    {
        throw new UnsupportedOperationException("Unsupported method");
    }
    
    /**
     * Return the number of geodesic paths between a node and the rest of nodes in the network.
     * @param node the node.
     * @return a map containing the number of geodesic paths from the node to the rest of the network.
     */
    public Map<U, Double> getGeodesics(U node)
    {
        throw new UnsupportedOperationException("Unsupported method");
    }

    /**
     * Returns the number of geodesic paths between two nodes.
     * @param orig origin node.
     * @param dest destination node.
     * @return the number of geodesic paths between both nodes if there is a path between them, 0.0 if not.
     */
    public double getGeodesics(U orig, U dest)
    {
        throw new UnsupportedOperationException("Unsupported method");
    }

    /**
     * Obtains the strongly connected components of the graph.
     * @return the strongly connected components of the graph.
     */
    public Communities<U> getSCC()
    {
        return this.scc;
    }
}
