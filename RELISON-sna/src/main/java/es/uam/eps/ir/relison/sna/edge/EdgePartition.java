/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.sna.edge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Class that relates graph edges with edge groups. Each edge can belong to at most one partition.
 *
 * @param <U> type of the nodes.
 */
public class EdgePartition<U>
{
    private final Map<Edge<U>, Integer> edgePartition;
    private final List<List<Edge<U>>> partitionEdges;

    public EdgePartition()
    {
        this.edgePartition = new HashMap<>();
        this.partitionEdges = new ArrayList<>();
    }

    /** @return the number of edge groups. */
    public int getNumPartitions()
    {
        return partitionEdges.size();
    }

    /** @return the identifiers of the edge groups. */
    public IntStream getPartitions()
    {
        return IntStream.range(0, getNumPartitions());
    }

    /**
     * Gets the group assigned to an edge.
     * @param edge edge reference.
     * @return the group identifier, or -1 if the edge is not present.
     */
    public int getPartition(Edge<U> edge)
    {
        return edgePartition.getOrDefault(edge, -1);
    }

    /**
     * Gets the edges assigned to a group.
     * @param partition group identifier.
     * @return a stream of edges in the group, empty if the identifier is invalid.
     */
    public Stream<Edge<U>> getEdges(int partition)
    {
        return partition >= 0 && partition < getNumPartitions()
                ? partitionEdges.get(partition).stream() : Stream.empty();
    }

    /**
     * Gets the number of edges in a group.
     * @param partition group identifier.
     * @return group size, or zero if the identifier is invalid.
     */
    public int getPartitionSize(int partition)
    {
        return partition >= 0 && partition < getNumPartitions() ? partitionEdges.get(partition).size() : 0;
    }

    /** Adds an empty edge group. */
    public void addPartition()
    {
        partitionEdges.add(new ArrayList<>());
    }

    /**
     * Adds an edge to a group if it is not already assigned.
     * @param edge edge reference.
     * @param partition existing group identifier.
     * @return true if the edge was assigned, false if the group is invalid or the edge is already assigned.
     */
    public boolean add(Edge<U> edge, int partition)
    {
        if (edge == null || partition < 0 || partition >= getNumPartitions() || edgePartition.containsKey(edge)) return false;
        partitionEdges.get(partition).add(edge);
        edgePartition.put(edge, partition);
        return true;
    }
}
