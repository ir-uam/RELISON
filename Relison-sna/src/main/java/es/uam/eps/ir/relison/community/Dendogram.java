/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.community;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.Weight;
import es.uam.eps.ir.relison.graph.generator.GraphGenerator;
import es.uam.eps.ir.relison.graph.generator.TreeCloneGenerator;
import es.uam.eps.ir.relison.graph.generator.exception.GeneratorBadConfiguredException;
import es.uam.eps.ir.relison.graph.generator.exception.GeneratorNotConfiguredException;
import es.uam.eps.ir.relison.graph.tree.Tree;
import es.uam.eps.ir.relison.graph.tree.fast.FastWeightedTree;
import es.uam.eps.ir.relison.index.fast.FastIndex;
import org.jooq.lambda.tuple.Tuple3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Abstract class that represents a dendogram and its functions.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Dendogram<U>
{
    /**
     * An index for relating the graph and the tree.
     */
    private final FastIndex<U> index;
    /**
     * The original graph
     */
    private final Graph<U> graph;
    /**
     * The dendogram tree. In this tree, nodes from 0 to numNodes-1 represent the leaf nodes
     * (i.e.: the nodes in the graph). Nodes from numNodes to 2*numNodes-2 represent the joints
     * of the dendogram.
     * <p>
     * Given an edge in the tree, the weight represents the number of leaves the child node is parent of.
     */
    private final Tree<Integer> tree;

    /**
     * Constructor.
     *
     * @param index    An index for identifying nodes in the graph, and nodes in the tree.
     * @param graph    The original graph.
     * @param triplets The dendogram tree. Ordered from root to leaves.
     */
    public Dendogram(FastIndex<U> index, Graph<U> graph, Stream<Tuple3<Integer, Integer, Integer>> triplets)
    {
        this.index = index;
        this.graph = graph;
        this.tree = new FastWeightedTree<>();

        triplets.forEach(t ->
        {
            if (this.tree.getVertexCount() == 0)
            {
                this.tree.addRoot(t.v3);
            }
            this.tree.addChild(t.v3, t.v1);
            this.tree.addChild(t.v3, t.v2);
        });

        int numVertices = (int) graph.getVertexCount();
        for (int i = 0; i < numVertices; ++i)
        {
            this.tree.updateEdgeWeight(this.tree.getParent(i), i, 1.0);
        }

        int numVerticesTree = (int) tree.getVertexCount();
        for (int i = numVertices; i < numVerticesTree; ++i)
        {
            double weight = this.tree.getChildrenWeights(i).mapToDouble(Weight::getValue).sum();
            this.tree.updateEdgeWeight(this.tree.getParent(i), i, weight);
        }
    }

    /**
     * Obtains the dendogram tree. To prevent from modifying the original tree, this provides a copy.
     *
     * @return The dendogram tree.
     */
    public Tree<Integer> getTree()
    {
        try
        {
            GraphGenerator<Integer> graphGen = new TreeCloneGenerator<>();
            graphGen.configure(this.tree);
            return (Tree<Integer>) graphGen.generate();
        }
        catch (GeneratorNotConfiguredException | GeneratorBadConfiguredException ex)
        {
            return null;
        }
    }

    /**
     * Obtains a community partition, containing, at most, a given number
     * of communities.
     *
     * @param n The maximum number of communities.
     *
     * @return the community partition, null if the number is invalid or something failed.
     */
    public Communities<U> getCommunitiesByNumber(int n)
    {
        if (n <= 0) // An error ocurred: n cannot be smaller than 1.
        {
            return null;
        }

        // Creating the communities
        Communities<U> comm = new Communities<>();
        // If we want more communities than nodes in the network, each node
        // belongs to a different community.
        if (n >= this.graph.getVertexCount())
        {
            this.graph.getAllNodes().forEach(u ->
            {
                comm.addCommunity();
                comm.add(u, comm.getNumCommunities() - 1);
            });
        }
        else if (n == 1)
        {
            comm.addCommunity();
            this.graph.getAllNodes().forEach(u -> comm.add(u, comm.getNumCommunities() - 1));
        }
        else // Algorithm
        {
            /* First, we select the initial node: starting from the root (1 division),
             * we select the (n-th - 1) fork in the tree. The node (root - n + 1) represents
             * the fork where all nodes were divided into n communities.*/
            int initial = this.tree.getRoot() - n + 1;
            int currentComms = 0;

            /*
             * We traverse the nodes in descending order.
             */
            for (int i = initial; currentComms < n; --i)
            {
                // If the identifier of the parent of the node is smaller than the initial node,
                // then, since the leaves for that node would have already been collected, so it is
                // discarded as a parent node for a community.
                // On the other hand, if the parent is greater than that other node, it means that 
                // the corresponding joint did not exist when the nodes were gathered in n comms.,
                // so the explored node is a parent node of a community.
                if (this.tree.getParent(i) != null && this.tree.getParent(i) > initial)
                {
                    comm.addCommunity();
                    if (this.tree.isLeaf(i))
                    {
                        comm.add(this.index.idx2object(i), comm.getNumCommunities() - 1);
                    }
                    else
                    {
                        this.tree.getLeaves(i).forEach(uIdx -> comm.add(this.index.idx2object(uIdx), comm.getNumCommunities() - 1));
                    }
                    currentComms++;
                }
            }
        }

        return comm;
    }

    /**
     * Gets all possible community partitions by number.
     *
     * @return A map containing all the possible partitions.
     */
    public Map<Integer, Communities<U>> getCommunitiesByNumber()
    {
        int root = this.tree.getRoot();
        int graphVertexCount = Long.valueOf(this.graph.getVertexCount()).intValue();
        int treeVertexCount = Long.valueOf(this.tree.getVertexCount()).intValue();
        Map<Integer, Communities<U>> map = new HashMap<>();
        Map<Integer, Set<U>> comm = new HashMap<>();
        for (int i = 1; i <= graph.getVertexCount(); ++i)
        {
            map.put(i, new Communities<>());
        }

        for (int i = 0; i < treeVertexCount; ++i)
        {
            // Incrementally find the communities.
            if (!comm.containsKey(i))
            {
                comm.put(i, new HashSet<>());
                comm.get(i).add(this.index.idx2object(i));
            }

            // If the tree is root, add to comm. size == 1 the rest
            if (!this.tree.isRoot(i))
            {
                int parent = this.tree.getParent(i);
                if (!comm.containsKey(parent))
                {
                    comm.put(parent, new HashSet<>());
                }
                comm.get(parent).addAll(comm.get(i));

                /*
                 * All joint nodes are labeled as (numNodes + numJoint), with numJoint starting by zero.
                 * Example: the first joint node is numNodes + 0, the second is numNodes + 1, etc.
                 *
                 * Since each joint combines two possible clusters, each time we select a new joint node,
                 * a division has to be discarded. Those divisions are those with more than
                 * (numNodes - numJoint) nodes. Since numJoint = node - numNodes, all divisions with the same
                 * or more number of communities than numNodes - [node - numNodes] are discarded.
                 *
                 * Then, the inferior limit is given by the parent of the node. In the individual version
                 * of the algorithm, it is observed that the node (root - numComms + 1) is the node where the
                 * dendogram is forked in numComms communities. The parent of the node represents the time where
                 * the community stops existing on its own. In that parent, the number of communities is equal to
                 * (root - parent + 1). So, for all divisions with equal or less communities than that number, the
                 * community formed by the leaves of the node will not be included.
                 *
                 * In conclusion, the community formed by the leaves of the current node will appear in the blocks
                 * between root - parent + 2 and min(numNodes - (node - numNodes) - 1, numNodes) (both included).
                 */
                int maxLimit = Math.min(graphVertexCount - (i - graphVertexCount) - 1, graphVertexCount);
                int minLimit = root - parent + 2;

                // For all items in the range, fill the communities.
                for (int j = minLimit; j <= maxLimit; ++j)
                {
                    Communities<U> comms = map.get(j);
                    comms.addCommunity();
                    comm.get(i).forEach(u -> comms.add(u, comms.getNumCommunities() - 1));
                }
            }
            else
            {
                map.get(1).addCommunity();
                this.graph.getAllNodes().forEach(v -> map.get(1).add(v, map.get(1).getNumCommunities() - 1));
            }
        }


        return map;
    }

    /**
     * Obtains a community partition, trying to provide communities with a given size.
     *
     * @param size The maximum size of the communities.
     *
     * @return the community partition, or null if something failed.
     */
    public Communities<U> getCommunitiesBySize(int size)
    {
        return this.getCommunitiesBySize(this.tree.getRoot(), size);
    }

    /**
     * Auxiliary recursive method.
     *
     * @param node The starting node.
     * @param size The maximum size of the communities.
     *
     * @return the community partition, or null if something failed.
     */
    private Communities<U> getCommunitiesBySize(int node, int size)
    {
        if (size <= 0)
        {
            return null;
        }

        double initialSize;
        Communities<U> comm = new Communities<>();
        if (this.tree.isRoot(node))
        {
            initialSize = this.graph.getVertexCount() + 0.0;
        }
        else
        {
            initialSize = this.tree.getParentWeight(node);
        }

        if (initialSize <= size)
        {
            comm.addCommunity();
            if (this.tree.isLeaf(node))
            {
                comm.add(this.index.idx2object(node), comm.getNumCommunities() - 1);
            }
            else
            {
                this.tree.getLeaves(node).forEach(uIdx -> comm.add(this.index.idx2object(uIdx), comm.getNumCommunities() - 1));
            }
        }
        else
        {
            this.tree.getChildren(node).forEach(child ->
            {
                Communities<U> aux = this.getCommunitiesBySize(child, size);
                aux.getCommunities().forEach(c ->
                {
                    comm.addCommunity();
                    aux.getUsers(c).forEach(u -> comm.add(u, comm.getNumCommunities() - 1));
                });
            });
        }

        return comm;
    }

    /**
     * Obtains all the possible community partitions, given the size of the communities.
     *
     * @return all the possible community partitions.
     */
    public Map<Integer, Communities<U>> getCommunitiesBySize()
    {
        int graphVertexCount = Long.valueOf(this.graph.getVertexCount()).intValue();
        int treeVertexCount = Long.valueOf(this.tree.getVertexCount()).intValue();
        Map<Integer, Communities<U>> map = new HashMap<>();
        Map<Integer, Set<U>> comm = new HashMap<>();
        for (int i = 1; i <= graphVertexCount; ++i)
        {
            map.put(i, new Communities<>());
        }

        IntStream.range(0, treeVertexCount).forEach(i ->
        {
            // Incrementally find the communities.
            if (!comm.containsKey(i))
            {
                comm.put(i, new HashSet<>());
                comm.get(i).add(this.index.idx2object(i));
            }


            // If the tree is root, add to comm. size == 1 the rest
            if (!this.tree.isRoot(i))
            {
                int parent = this.tree.getParent(i);
                if (!comm.containsKey(parent))
                {
                    comm.put(parent, new HashSet<>());
                }
                comm.get(parent).addAll(comm.get(i));

                int minLimit = Double.valueOf(this.tree.getParentWeight(i)).intValue();
                int maxLimit;

                if (this.tree.isRoot(parent))
                {
                    maxLimit = graphVertexCount;
                }
                else
                {
                    maxLimit = Double.valueOf(this.tree.getParentWeight(parent)).intValue();
                }

                IntStream.range(minLimit, maxLimit).forEach(j -> {
                    Communities<U> comms = map.get(j);
                    comms.addCommunity();
                    comm.get(i).forEach(u -> comms.add(u, comms.getNumCommunities() - 1));
                });
            }
            else
            {
                Communities<U> comms = map.get(graphVertexCount);
                comms.addCommunity();
                this.graph.getAllNodes().forEach(v -> comms.add(v, comms.getNumCommunities() - 1));
            }
        });


        return map;

    }
}
