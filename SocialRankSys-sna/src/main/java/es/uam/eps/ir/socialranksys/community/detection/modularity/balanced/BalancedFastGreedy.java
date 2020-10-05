/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.community.detection.modularity.balanced;

import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.socialranksys.community.detection.connectedness.WeaklyConnectedComponents;
import es.uam.eps.ir.socialranksys.community.graph.CompleteCommunityGraphGenerator;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.generator.EmptyGraphGenerator;
import es.uam.eps.ir.socialranksys.graph.generator.GraphGenerator;
import es.uam.eps.ir.socialranksys.graph.generator.SubGraphGenerator;
import es.uam.eps.ir.socialranksys.graph.generator.exception.GeneratorBadConfiguredException;
import es.uam.eps.ir.socialranksys.graph.generator.exception.GeneratorNotConfiguredException;
import es.uam.eps.ir.socialranksys.graph.multigraph.MultiGraph;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Community detection algorithm for obtaining balanced communities, based on the FastGreedy algorithm
 * by M.E.J. Newman.
 *
 * <p>
 * <b>References:</b></p>
 *     <ol>
 *         <li>M.E.J. Newman. Fast Algorithm for detecting community structure in networks. Physical Review E 69(6): 066133 (2004)</li>
 *         <li>Huang, M., Nguyen, Q. A Fast Algorithm For Balanced Graph Clustering. 11th International IEEE Conference on Information Visualization (IV 2007), Zurich, Switzerland (2007) </li>
 *     </ol>
 *
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class BalancedFastGreedy<U> implements CommunityDetectionAlgorithm<U>
{
    /**
     * Maximum community size.
     */
    private final int commSize;

    /**
     * Constructor.
     *
     * @param commSize Maximum community size.
     */
    public BalancedFastGreedy(int commSize)
    {
        this.commSize = commSize;
    }

    @Override
    public Communities<U> detectCommunities(Graph<U> graph)
    {
        Communities<U> comm = new Communities<>();
        Graph<U> auxGraph;
        if (graph.isDirected()) //If the graph is directed, build the undirected equivalent.
        {
            try
            {
                GraphGenerator<U> graphGen = new EmptyGraphGenerator<>();
                graphGen.configure(false, false);
                auxGraph = graphGen.generate();

                graph.getAllNodes().forEach(auxGraph::addNode);
                graph.getAllNodes().forEach(u -> graph.getAdjacentNodes(u).forEach(v -> auxGraph.addEdge(u, v)));
            }
            catch (GeneratorNotConfiguredException | GeneratorBadConfiguredException ex)
            {
                return comm;
            }

        }
        else
        {
            auxGraph = graph;
        }


        if (commSize == 1)
        {
            graph.getAllNodes().forEach(u ->
                                        {
                                            comm.addCommunity();
                                            comm.add(u, comm.getNumCommunities() - 1);
                                        });
        }
        else if (commSize > 1)
        {
            // First, we identify the strongly connected components of the graph.
            CommunityDetectionAlgorithm<U> wccDet = new WeaklyConnectedComponents<>();
            Communities<U> wcc = wccDet.detectCommunities(auxGraph);

            comm.addCommunity();
            comm.addCommunity();
            List<Set<U>> auxList = new ArrayList<>();

            // For each strongly connected algorithm, apply:
            wcc.getCommunities().forEach(cc ->
            {
                long size = wcc.getCommunitySize(cc);
                if (size >= commSize)
                {
                    auxList.add(new HashSet<>());
                }

                // Initialize the clusters
                wcc.getUsers(cc).forEach(u ->
                {
                    if (size == 1) // We group all isolated nodes in a single cluster.
                    {
                        comm.add(u, comm.getNumCommunities() - 2);
                    }
                    else if (size < commSize) // We group all small groups (< commSize) in a single cluster.
                    {
                        comm.add(u, comm.getNumCommunities() - 1);
                    }
                    else // Initialize the community detection for large clusters.
                    {
                        auxList.get(auxList.size() - 1).add(u);
                    }
              });
            });

            // Apply the clustering algorithm on the large connected components.
            for (Set<U> largec : auxList)
            {
                Communities<U> aux = this.cluster(auxGraph, largec);
                if (aux != null)
                {
                    aux.getCommunities().forEach(c ->
                    {
                        comm.addCommunity();
                        aux.getUsers(c).forEach(u -> comm.add(u, comm.getNumCommunities() - 1));
                    });
                }
                else
                {
                    return null;
                }
            }
        }

        return comm;

    }

    /**
     * Applies the clustering algorithm to a large group of nodes.
     *
     * @param graph The graph.
     * @param users The large group of nodes.
     *
     * @return A community partition if everything went OK, null if not.
     */
    private Communities<U> cluster(Graph<U> graph, Set<U> users)
    {
        try
        {
            Communities<U> comm = new Communities<>();
            GraphGenerator<U> subgraphGen = new SubGraphGenerator<>();
            subgraphGen.configure(graph, users);
            Graph<U> subgraph = subgraphGen.generate();

            // Initializing the clustering: each user belongs to a separate community.
            Communities<U> aux = new Communities<>();
            for (U u : users)
            {
                aux.addCommunity();
                aux.add(u, aux.getNumCommunities() - 1);
            }

            // We generate the community graph.
            CompleteCommunityGraphGenerator<U> commGraphGen = new CompleteCommunityGraphGenerator<>();
            MultiGraph<Integer> commGraph;

            double maxDeltaQ;
            Pair<Integer> maxPair;
            List<Integer> comms;

            do
            {
                comms = aux.getCommunities().boxed().collect(Collectors.toCollection(ArrayList::new));
                // Generate the community graph (for identifying the number of edges between communities)
                commGraph = commGraphGen.generate(graph, aux);
                maxDeltaQ = 0;
                maxPair = null;

                // Average size before a group merging.
                double averageBefore = 0.0;
                // Average size after a group merging -> the number of nodes remains the same, just divide
                // by the number of clusters - 1.
                double averageAfter;
                for (int c : comms)
                {
                    averageBefore += (aux.getCommunitySize(c) + 0.0) / (subgraph.getVertexCount() + 0.0);
                }
                averageAfter = averageBefore / (aux.getNumCommunities() - 1.0);
                averageBefore /= (aux.getNumCommunities() + 0.0);

                // For eah pair of communities (i,j), compute the improvement in modularity:
                // \Delta Q = 2(e_ij - a_i a_j) - wd_ij
                for (int i = 0; i < comms.size(); ++i)
                {
                    int sizeCommI = aux.getCommunitySize(i);
                    double edgesFromI = commGraph.getNeighbourEdgesCount(i) / (subgraph.getEdgeCount() + 0.0);
                    double weightI = (sizeCommI + 0.0) / (subgraph.getVertexCount() + 0.0);

                    for (int j = 0; j < i; ++j)
                    {
                        int sizeCommJ = aux.getCommunitySize(j);
                        double edgesFromJ = commGraph.getNeighbourEdgesCount(j) / (subgraph.getEdgeCount() + 0.0);
                        double edgesIJ = 0.0;

                        if (commGraph.containsEdge(i, j))
                        {
                            edgesIJ = commGraph.getEdgeWeights(i, j).size() / (subgraph.getEdgeCount() + 0.0);
                        }

                        double weightJ = (sizeCommJ + 0.0) / (subgraph.getVertexCount() + 0.0);

                        // wd_ij = |\frac{|U_i|}{|U|} + \frac{|U_j|}{|U|} - avg(|
                        double wd = Math.abs(weightI + weightJ - averageAfter) - 0.5 * Math.abs(weightI - averageBefore) - 0.5 * Math.abs(weightJ - averageBefore);
                        double deltaQ = 2 * (edgesIJ - edgesFromI * edgesFromJ) - wd;

                        if (deltaQ > maxDeltaQ)
                        {
                            maxDeltaQ = deltaQ;
                            maxPair = new Pair<>(i, j);
                        }
                    }
                }

                if (maxDeltaQ > 0)
                {
                    int firstComm = maxPair.v1();
                    int secondComm = maxPair.v2();
                    Communities<U> aux2 = new Communities<>();
                    for (int c : comms)
                    {
                        if (c != secondComm)
                        {
                            aux2.addCommunity();
                            aux.getUsers(c).forEach(u -> aux2.add(u, aux2.getNumCommunities() - 1));
                        }

                        // Merge the communities
                        if (c == firstComm)
                        {
                            aux.getUsers(secondComm).forEach(u -> aux2.add(u, aux2.getNumCommunities() - 1));
                        }
                    }

                    aux = aux2;
                }


                // MultiGraph<Integer> commGraph = commGraphGen.generate(graph, comm);
            }
            while (maxDeltaQ > 0 && aux.getNumCommunities() > 2);

            comms = aux.getCommunities().boxed().collect(Collectors.toCollection(ArrayList::new));

            for (int c : comms)
            {
                if (aux.getCommunitySize(c) > commSize)
                {
                    Stream<U> us = aux.getUsers(c);
                    Communities<U> smallerGrain = this.cluster(subgraph, aux.getUsers(c).collect(Collectors.toSet()));
                    smallerGrain.getCommunities().forEach(c1 ->
                    {
                        comm.addCommunity();
                        smallerGrain.getUsers(c1).forEach(u -> comm.add(u, comm.getNumCommunities() - 1));
                    });
                }
                else
                {
                    comm.addCommunity();
                    aux.getUsers(c).forEach(u -> comm.add(u, comm.getNumCommunities() - 1));
                }
            }

            return comm;
        }
        catch (GeneratorNotConfiguredException | GeneratorBadConfiguredException ex)
        {
            return null;
        }


    }
}
