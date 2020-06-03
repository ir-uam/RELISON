/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.data.updateable.index.fast;

import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.data.GraphIndex;

import java.util.stream.Stream;

/**
 * Class that represents both user and item indexes for a graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public class FastUpdateableGraphIndex<U> implements GraphIndex<U>, FastUpdateableUserIndex<U>, FastUpdateableItemIndex<U>
{
    /**
     * User index.
     */
    private final FastGraph<U> graph;

    /**
     * Constructor. From a FastGraph, extracts the information.
     * @param graph the graph.
     */
    public FastUpdateableGraphIndex(FastGraph<U> graph)
    {
        this.graph = graph;
    }
    
    @Override
    public int user2uidx(U u) 
    {
        return this.graph.getIndex().object2idx(u);
    }

    @Override
    public U uidx2user(int i) 
    {
        return this.graph.getIndex().idx2object(i);
    }

    @Override
    public boolean containsUser(U u) 
    {
        return this.graph.containsVertex(u);
    }

    @Override
    public int numUsers() 
    {
        return this.graph.getIndex().numObjects();
    }

    @Override
    public Stream<U> getAllUsers() 
    {
        return this.graph.getAllNodes();
    }

    @Override
    public Stream<U> getAllItems() { return this.graph.getAllNodes();}

    @Override
    public int item2iidx(U i) 
    {
        return this.user2uidx(i);
    }

    @Override
    public int addItem(U u) {
        graph.addNode(u);
        return graph.object2idx(u);
    }

    @Override
    public int addUser(U u) {
        graph.addNode(u);
        return graph.object2idx(u);
    }
}
