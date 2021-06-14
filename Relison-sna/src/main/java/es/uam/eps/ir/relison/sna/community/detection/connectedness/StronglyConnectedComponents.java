/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.sna.community.detection.connectedness;

import es.uam.eps.ir.relison.sna.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.graph.Graph;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Computes communities via the Strongly Connected Components.
 *
 * @param <U> Type of the users.
 *
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class StronglyConnectedComponents<U> implements CommunityDetectionAlgorithm<U>
{
    private int time = 0;

    @Override
    public Communities<U> detectCommunities(Graph<U> graph)
    {
        Collection<Collection<U>> scc = this.findSCC(graph);
        Communities<U> comm = new Communities<>();

        int i = 0;
        for (Collection<U> cc : scc)
        {
            comm.addCommunity();
            for (U u : cc)
            {
                comm.add(u, i);
            }
            ++i;
        }
        return comm;

    }

    /**
     * Finds the strongly connected components of the graph.
     *
     * @param g The graph.
     *
     * @return The strongly connected clusters of the graph.
     */
    private Collection<Collection<U>> findSCC(Graph<U> g)
    {
        Set<U> auxDiscovered = new HashSet<>();
        final Map<U, Integer> processed = new HashMap<>();

        g.getAllNodes().forEach(u -> {
            if (!auxDiscovered.contains(u))
            {
                visit(u, g, auxDiscovered, processed);
            }
        });

        Set<U> discovered;

        List<U> vertices = g.getAllNodes().sorted((U u, U v) -> processed.get(v) - processed.get(u)).collect(Collectors.toCollection(ArrayList::new));

        Collection<Collection<U>> components = new HashSet<>();
        discovered = new HashSet<>();
        for (U u : vertices)
        {
            if (!discovered.contains(u))
            {
                Collection<U> component = new HashSet<>()
                {
                    @Override
                    public boolean equals(Object obj)
                    {
                        return this == obj;
                    }
                };
                transposedVisit(u, g, discovered, component);
                components.add(component);
            }
        }
        return components;
    }

    /**
     * Visits a node by using the outlinks
     *
     * @param u          The starting node.
     * @param g          The graph.
     * @param discovered The discovered items.
     * @param processed  The processed items.
     */
    private void visit(U u, Graph<U> g, Set<U> discovered, Map<U, Integer> processed)
    {
        discovered.add(u);
        g.getAdjacentNodes(u).forEach(v -> {
            if (!discovered.contains(v))
            {
                visit(v, g, discovered, processed);
            }
        });

        if (processed != null)
        {
            processed.put(u, time++);
        }
    }

    /**
     * Visits a node by using the inlinks
     *
     * @param u          The starting node.
     * @param g          The graph.
     * @param discovered The dsiscovered items.
     * @param component  The component.
     */
    private void transposedVisit(U u, Graph<U> g, Set<U> discovered, Collection<U> component)
    {
        component.add(u);
        discovered.add(u);
        g.getIncidentNodes(u).forEach(v -> {
            if (!discovered.contains(v))
            {
                transposedVisit(v, g, discovered, component);
            }
        });
    }


}
