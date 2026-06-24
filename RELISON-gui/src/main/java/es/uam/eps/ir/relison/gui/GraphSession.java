/*
 *  Copyright (C) 2023 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.gui;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.sna.metrics.distance.CompleteDistanceCalculator;
import es.uam.eps.ir.relison.sna.metrics.distance.DistanceCalculator;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds a single loaded network together with the auxiliary structures the GUI computes over it
 * (a shared distance calculator and the community partitions detected so far).
 *
 * <p>The distance calculator is expensive to build, so it is created lazily and reused across all
 * distance-based metrics, exactly as {@code GraphAnalyzer} does. Whenever the graph is edited, the
 * cached calculator and the stored partitions must be invalidated via {@link #invalidateCaches()}.</p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class GraphSession
{
    /** Unique identifier of this session. */
    private final String id;
    /**
     * The loaded network. Nodes are kept as {@link String} identifiers so the GUI accepts arbitrary id types
     * (numeric or textual); RELISON's metrics are generic, so this does not affect any computation.
     */
    private final Graph<String> graph;

    private final boolean directed;
    private final boolean weighted;
    private final boolean multigraph;
    private final boolean selfloops;

    /** Lazily-built distance calculator shared by all distance-based metrics. */
    private DistanceCalculator<String> distanceCalculator;
    /** Community partitions detected during the session, keyed by a user-facing name. */
    private final Map<String, Communities<String>> communities = new HashMap<>();

    /**
     * Constructor.
     * @param id         the session identifier.
     * @param graph      the loaded network.
     * @param directed   whether the network is directed.
     * @param weighted   whether the network is weighted.
     * @param multigraph whether the network allows multiple edges between a pair of nodes.
     * @param selfloops  whether the network allows self-loops.
     */
    public GraphSession(String id, Graph<String> graph, boolean directed, boolean weighted, boolean multigraph, boolean selfloops)
    {
        this.id = id;
        this.graph = graph;
        this.directed = directed;
        this.weighted = weighted;
        this.multigraph = multigraph;
        this.selfloops = selfloops;
    }

    public String getId()
    {
        return id;
    }

    public Graph<String> getGraph()
    {
        return graph;
    }

    public boolean isDirected()
    {
        return directed;
    }

    public boolean isWeighted()
    {
        return weighted;
    }

    public boolean isMultigraph()
    {
        return multigraph;
    }

    public boolean allowsSelfLoops()
    {
        return selfloops;
    }

    public Map<String, Communities<String>> getCommunities()
    {
        return communities;
    }

    /**
     * Returns the shared distance calculator, created lazily on first access.
     *
     * <p>Distances are <em>not</em> computed here: distance-based metrics call
     * {@code computeDistances(graph)} themselves (which is idempotent for a given graph), so reusing this single
     * instance means the expensive all-pairs computation happens at most once per session, while non-distance
     * metrics that ignore the calculator pay nothing.</p>
     *
     * @return the shared distance calculator for the current graph.
     */
    public DistanceCalculator<String> getDistanceCalculator()
    {
        if (distanceCalculator == null)
        {
            distanceCalculator = new CompleteDistanceCalculator<>();
        }
        return distanceCalculator;
    }

    /**
     * Drops everything that depends on the structure of the graph. Must be called after every edit so the next
     * metric or community computation runs against the modified network.
     */
    public void invalidateCaches()
    {
        this.distanceCalculator = null;
        this.communities.clear();
    }
}
