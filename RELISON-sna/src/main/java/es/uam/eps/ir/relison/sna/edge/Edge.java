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

import java.util.Objects;

/**
 * Reference to an edge in a RELISON graph. For multigraphs, the edge index and optional stable identifier
 * distinguish parallel edges between the same pair of nodes.
 *
 * @param <U> type of the nodes.
 */
public final class Edge<U>
{
    private final U source;
    private final U target;
    private final int index;
    private final Long id;
    private final boolean directed;

    /**
     * Builds an edge reference for a simple graph.
     * @param source source node.
     * @param target target node.
     * @param directed whether the edge is directed.
     */
    public Edge(U source, U target, boolean directed)
    {
        this(source, target, -1, null, directed);
    }

    /**
     * Builds an edge reference for a multigraph.
     * @param source source node.
     * @param target target node.
     * @param index edge index between the endpoint pair.
     * @param id optional stable edge identifier.
     * @param directed whether the edge is directed.
     */
    public Edge(U source, U target, int index, Long id, boolean directed)
    {
        this.source = Objects.requireNonNull(source);
        this.target = Objects.requireNonNull(target);
        this.index = index;
        this.id = id;
        this.directed = directed;
    }

    public U getSource() { return source; }
    public U getTarget() { return target; }
    public int getIndex() { return index; }
    public Long getId() { return id; }
    public boolean isDirected() { return directed; }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof Edge<?>)) return false;
        Edge<?> edge = (Edge<?>) other;
        if (directed != edge.directed) return false;
        boolean sameEndpoints = directed
                ? Objects.equals(source, edge.source) && Objects.equals(target, edge.target)
                : (Objects.equals(source, edge.source) && Objects.equals(target, edge.target))
                    || (Objects.equals(source, edge.target) && Objects.equals(target, edge.source));
        if (!sameEndpoints) return false;
        return id != null || edge.id != null ? Objects.equals(id, edge.id) : index == edge.index;
    }

    @Override
    public int hashCode()
    {
        int endpoints = directed ? 31 * Objects.hashCode(source) + Objects.hashCode(target)
                : Objects.hashCode(source) + Objects.hashCode(target);
        return Objects.hash(endpoints, directed, id == null ? index : id);
    }

    @Override
    public String toString()
    {
        return source + (directed ? " -> " : " -- ") + target + (id == null ? " [" + index + "]" : " [" + id + "]");
    }
}
