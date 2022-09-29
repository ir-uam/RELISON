/*
 *  Copyright (C) 2022-2022 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es / Terrier Team at University of Glasgow,
 *  http://http://terrierteam.dcs.gla.ac.uk
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.sna.edgegroups.simple;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.jooq.lambda.tuple.Tuple2;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Class for defining a partition of edges in a simple graph (only one edge between
 * two nodes).
 *
 * @param <U> type of the nodes.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzadopuig@glasgow.ac.uk)
 */
public class SimpleEdgePartition<U>
{
    /**
     * Indicates the community the user belongs to.
     */
    private final Object2IntMap<Tuple2<U, U>> edgeGroup;
    /**
     * Indicates the list of users of the communities.
     */
    private final Int2ObjectMap<List<Tuple2<U,U>>> commEdges;

    /**
     * Indicates whether the edges to store are directed or not.
     */
    private final boolean directed;

    /**
     * Constructor.
     */
    public SimpleEdgePartition(boolean directed)
    {
        edgeGroup = new Object2IntOpenHashMap<>();
        commEdges = new Int2ObjectOpenHashMap<>();
        edgeGroup.defaultReturnValue(-1);
        this.directed = directed;
    }

    /**
     * Obtain the number of edge groups.
     * @return the number of edge groups.
     */
    public int getNumGroups()
    {
        return edgeGroup.size();
    }

    /**
     * Obtains the different groups of edges on the partition.
     * @return an int stream containing the diferent edge groups of the graph.
     */
    public IntStream getEdgeGroups()
    {
        return commEdges.keySet().intStream();
    }

    /**
     * Gets the community a given user belongs to.
     *
     * @param edge The edge.
     *
     * @return The grouping if the edge has an associated group, -1 if it does not exist.
     */
    public int getGroup(Tuple2<U,U> edge)
    {
        return edgeGroup.getInt(edge);
    }

    /**
     * Gets the edges inside a group.
     *
     * @param group The group.
     *
     * @return a stream containing the edges in the grouping if exists, an empty stream if not.
     */
    public Stream<Tuple2<U,U>> getEdges(int group)
    {
        if (commEdges.containsKey(group))
        {
            return commEdges.get(group).stream();
        }
        else
        {
            return Stream.empty();
        }
    }

    /**
     * Adds a new group to the partition.
     *
     * @param id identifier of the group.
     *
     * @return true if the group was added, false otherwise (for instance, if it exists).
     * */
    public boolean addGroup(int id)
    {
        if(!this.commEdges.containsKey(id))
        {
            this.commEdges.put(id, new ArrayList<>());
            return true;
        }
        return false;
    }

    /**
     * Adds a edge/group pair.
     *
     * @param edge  The new edge. It must not be already in the object.
     * @param group The associated group. it has to previously exist.
     *
     * @return true if everything goes OK, false if not.
     */
    public boolean add(Tuple2<U,U> edge, int group)
    {
        Tuple2<U,U> reciprEdge = this.directed ? edge : new Tuple2<>(edge.v2, edge.v1);
        if (this.commEdges.containsKey(group) && !edgeGroup.containsKey(edge) && !edgeGroup.containsKey(reciprEdge))
        {
            this.commEdges.get(group).add(edge);
            this.edgeGroup.put(edge, group);
            return true;
        }
        return false;
    }

    /**
     * Obtains the size of an edge group.
     *
     * @param group The edge group whose size we want to obtain.
     *
     * @return the size of the group if it exists, 0 if it does not.
     */
    public int getEdgeGroupSize(int group)
    {
        if (this.commEdges.containsKey(group))
        {
            return commEdges.get(group).size();
        }
        else
        {
            return 0;
        }
    }

    /**
     * Checks whether a group exists or not in the partition.
     * @param group the identifier of the group.
     * @return true if the group exists, false otherwise.
     */
    public boolean hasGroup(int group)
    {
        return this.commEdges.containsKey(group);
    }

    /**
     * Determines whether the edges are directed or not.
     * @return true if the edges are directed, false otherwise.
     */
    public boolean isDirected()
    {
        return directed;
    }
}
