/* 
 * Copyright (C) 2018 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.community.detection.connectedness;

import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.socialranksys.graph.Graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Computes communities via the Strongly Connected Components
 * @author Pablo Castells Azpilicueta
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public class WeaklyConnectedComponents<U> implements CommunityDetectionAlgorithm<U>
{
    @Override
    public Communities<U> detectCommunities(Graph<U> graph)
    {
        Collection<Collection<U>> scc = this.findWCC(graph);
        Communities<U> comm = new Communities<>();
        
        int i = 0;
        for(Collection<U> cc : scc)
        {
            comm.addCommunity();
            for(U u : cc)
            {
                comm.add(u, i);
            }
            ++i;
        }       
        return comm;
        
    }
    
    /**
     * Finds the weakly connected components of the graph.
     * @param g The graph
     * @return The weakly connected clusters of the graph.
     */
    private Collection<Collection<U>> findWCC (Graph<U> g)
    {
        Set<U> discovered = new HashSet<>();
        Collection<Collection<U>> components = new ArrayList<>();
        g.getAllNodes().forEach(u -> 
        {
            if(!discovered.contains(u))
            {
                Collection<U> component = new HashSet<>()
                {
                    @Override
                    public boolean equals(Object obj)
                    {
                        return this == obj;
                    }
                };
                visit(u, g, discovered, component);
                components.add(component);
            }
        });
        
        return components;
    }

    /**
     * Visits a node by using the inlinks
     * @param u The starting node
     * @param g The graph
     * @param discovered The dsiscovered items
     * @param component The component
     */
    private void visit (U u, Graph<U> g, Set<U> discovered, Collection<U> component)
    {
        component.add(u);
        discovered.add(u);
        g.getNeighbourNodes(u).forEach(v -> 
        {
            if (!discovered.contains(v)) visit(v, g, discovered, component);
        });            
    }

    
}
