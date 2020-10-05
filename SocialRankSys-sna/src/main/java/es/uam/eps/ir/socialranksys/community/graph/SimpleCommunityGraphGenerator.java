/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package es.uam.eps.ir.socialranksys.community.graph;

import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeWeight;
import es.uam.eps.ir.socialranksys.graph.generator.EmptyGraphGenerator;
import es.uam.eps.ir.socialranksys.graph.generator.GraphGenerator;
import es.uam.eps.ir.socialranksys.graph.generator.exception.GeneratorBadConfiguredException;
import es.uam.eps.ir.socialranksys.graph.generator.exception.GeneratorNotConfiguredException;

import java.util.HashSet;
import java.util.Set;

/**
 * Generates a community graph, which has, at most, a single link between
 * each pair of communities (including auto-loops). The weight of the corresponding
 * graph is the sum of the weight of the links between communities.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class SimpleCommunityGraphGenerator<U> implements GraphGenerator<Integer>
{
    /**
     * The graph.
     */
    private Graph<U> graph;
    /**
     * A community partition.
     */
    private Communities<U> comm;
    /**
     * true if we have to take into account the real directionality of the graph,
     * false if the graph has to be undirected.
     */
    private boolean directed;
    /**
     * true if the generator has been configured, false otherwise.
     */
    private boolean configured = false;

    @Override
    public void configure(Object... configuration)
    {
        if (!(configuration == null) && configuration.length == 3)
        {
            Graph<U> auxGraph = (Graph<U>) configuration[0];
            Communities<U> auxComms = (Communities<U>) configuration[1];
            boolean auxDirected = (boolean) configuration[2];
            this.configure(auxGraph, auxComms, auxDirected);
        }
        else
        {
            this.configured = false;
        }
    }

    /**
     * Configures the generator.
     *
     * @param graph    the graph.
     * @param comm     the community partition
     * @param directed true if we have to take communities into account
     */
    public void configure(Graph<U> graph, Communities<U> comm, boolean directed)
    {
        this.graph = graph;
        this.comm = comm;
        this.configured = true;
        this.directed = directed;
    }


    @Override
    public Graph<Integer> generate() throws GeneratorNotConfiguredException, GeneratorBadConfiguredException
    {
        if (!configured)
        {
            throw new GeneratorNotConfiguredException("Generator has not been correctly configured");
        }
        else
        {
            if (graph == null || comm == null)
            {
                throw new GeneratorBadConfiguredException("Generator has been badly configured");
            }


            // First, we configure the graph generator.
            EmptyGraphGenerator<Integer> gg = new EmptyGraphGenerator<>();
            boolean isDirected = graph.isDirected();

            if (this.directed)
            {
                gg.configure(isDirected, true);
            }
            else
            {
                gg.configure(false, true);
            }

            // Generate the comm. graph
            Graph<Integer> commGraph = gg.generate();
            this.comm.getCommunities().forEach(commGraph::addNode);

            if (isDirected) // if the graph is directed
            {
                graph.getAllNodes().forEach(orig -> graph.getAdjacentNodesWeights(orig).forEach(dest ->
                {
                    int origComm = comm.getCommunity(orig);
                    int destComm = comm.getCommunity(dest.getIdx());
                    double oldVal = commGraph.getEdgeWeight(origComm, destComm);

                    if (EdgeWeight.isErrorValue(oldVal))
                    {
                        commGraph.addEdge(origComm, destComm, dest.getValue());
                    }
                    else
                    {
                        commGraph.updateEdgeWeight(origComm, destComm, oldVal + dest.getValue());
                    }
                }));
            }
            else // otherwise
            {
                Set<U> visited = new HashSet<>();
                graph.getAllNodes().forEach(orig ->
                {
                    graph.getAdjacentNodesWeights(orig).forEach(dest ->
                    {
                        if (!visited.contains(dest.getIdx()))
                        {
                            int origComm = comm.getCommunity(orig);
                            int destComm = comm.getCommunity(dest.getIdx());
                            double oldVal = commGraph.getEdgeWeight(origComm, destComm);

                            if (!EdgeWeight.isErrorValue(oldVal))
                            {
                                commGraph.addEdge(origComm, destComm, dest.getValue());
                            }
                            else
                            {
                                commGraph.updateEdgeWeight(origComm, destComm, oldVal + dest.getValue());
                            }

                        }
                    });
                    visited.add(orig);
                });
            }

            return commGraph;
        }
    }
}
