/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.community.graph;

import es.uam.eps.ir.sonalire.community.Communities;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.graph.generator.EmptyMultiGraphGenerator;
import es.uam.eps.ir.sonalire.graph.generator.exception.GeneratorNotConfiguredException;
import es.uam.eps.ir.sonalire.graph.multigraph.MultiGraph;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates a multi-graph which contains all communities as nodes and all links between
 * communities (different or not) as edges. Self-loops in the original network do not generate links
 * in the community network.
 *
 * @param <U> type of the nodes
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class CompleteCommunityNoSelfLoopsGraphGenerator<U> implements CommunityGraphGenerator<U>
{
    @Override
    public MultiGraph<Integer> generate(Graph<U> graph, Communities<U> communities)
    {
        try
        {
            EmptyMultiGraphGenerator<Integer> gg = new EmptyMultiGraphGenerator<>();

            boolean directed = graph.isDirected();
            boolean weighted = false;

            gg.configure(directed, weighted);
            MultiGraph<Integer> commGraph = (MultiGraph<Integer>) gg.generate();

            communities.getCommunities().forEach(commGraph::addNode);

            if (directed)
            {
                graph.getAllNodes().forEach((orig) -> graph.getAdjacentNodes(orig).forEach((dest) ->
                {
                    if (orig != dest)
                    {
                        int origComm = communities.getCommunity(orig);
                        int destComm = communities.getCommunity(dest);
                        commGraph.addEdge(origComm, destComm);
                    }
                }));
            }
            else
            {
                List<U> visited = new ArrayList<>();
                graph.getAllNodes().forEach((orig) ->
                {
                    graph.getAdjacentNodes(orig).forEach((dest) ->
                    {
                        if (!visited.contains(dest) && orig != dest)
                        {
                            int origComm = communities.getCommunity(orig);
                            int destComm = communities.getCommunity(dest);
                            commGraph.addEdge(origComm, destComm);
                        }
                    });
                    visited.add(orig);
                });

            }

            return commGraph;
        }
        catch (GeneratorNotConfiguredException ex)
        {
            return null;
        }
    }
}
