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
import es.uam.eps.ir.relison.graph.generator.GraphGenerator;
import es.uam.eps.ir.relison.graph.generator.GraphCloneGenerator;
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.sna.metrics.distance.CompleteDistanceCalculator;
import es.uam.eps.ir.relison.sna.metrics.distance.DistanceCalculator;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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

    /** Recommendations/predictions computed during the session, keyed by their model signature. */
    private final Map<String, RecommendationResult> recommendations = new LinkedHashMap<>();
    /** The signature of the recommendation currently overlaid on the graph, or {@code null} if none. */
    private String activeRecommendation;
    /** Lazily-built graph = base graph + the active recommendation's edges (for "with recommendation" metrics). */
    private Graph<String> augmentedGraph;
    /** Lazily-built distance calculator for {@link #augmentedGraph}. */
    private DistanceCalculator<String> augmentedDistanceCalculator;

    /** The most recent information-diffusion simulation result, or {@code null}. */
    private DiffusionResult diffusion;
    /** The information pieces defined for diffusion (each a map with {@code id}, {@code creator}, {@code timestamp}). */
    private List<?> diffusionPieces = List.of();

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

    /** @return the most recent diffusion result, or {@code null}. */
    public DiffusionResult getDiffusion()
    {
        return diffusion;
    }

    /** Stores (or clears, with {@code null}) the most recent diffusion result. */
    public void setDiffusion(DiffusionResult diffusion)
    {
        this.diffusion = diffusion;
    }

    /** @return the persisted diffusion information pieces (never {@code null}). */
    public List<?> getDiffusionPieces()
    {
        return diffusionPieces;
    }

    /** Persists the diffusion information pieces on the session. */
    public void setDiffusionPieces(List<?> pieces)
    {
        this.diffusionPieces = pieces == null ? List.of() : pieces;
    }

    /** @return the recommendations computed so far, keyed by their model signature. */
    public Map<String, RecommendationResult> getRecommendations()
    {
        return recommendations;
    }

    /** @return the active recommendation, or {@code null} if none is overlaid. */
    public RecommendationResult getActiveRecommendation()
    {
        return activeRecommendation == null ? null : recommendations.get(activeRecommendation);
    }

    /** @return the signature of the active recommendation, or {@code null}. */
    public String getActiveRecommendationKey()
    {
        return activeRecommendation;
    }

    /**
     * Sets (or clears, with {@code null}) the recommendation overlaid on the graph and drops the cached augmented
     * graph so the next "with recommendation" metric is computed against the new overlay.
     * @param key the signature of the recommendation to activate, or {@code null} to clear it.
     */
    public void setActiveRecommendation(String key)
    {
        this.activeRecommendation = key;
        this.augmentedGraph = null;
        this.augmentedDistanceCalculator = null;
    }

    /**
     * Returns the base graph augmented with the active recommendation's edges, built lazily and cached. The base
     * graph is never modified: a clone is created and the recommended links are added to it.
     * @return the augmented graph, or {@code null} if there is no active recommendation.
     */
    public Graph<String> getAugmentedGraph()
    {
        RecommendationResult active = getActiveRecommendation();
        if (active == null) return null;
        if (augmentedGraph == null)
        {
            try
            {
                GraphGenerator<String> cloner = new GraphCloneGenerator<>();
                cloner.configure(graph);
                Graph<String> aug = cloner.generate();
                for (RecommendationResult.RecEdge e : active.edges)
                {
                    if (!aug.containsEdge(e.source, e.target))
                    {
                        aug.addEdge(e.source, e.target);
                    }
                }
                augmentedGraph = aug;
            }
            catch (Exception ex)
            {
                throw new RuntimeException("Could not build the augmented graph: " + ex.getMessage(), ex);
            }
        }
        return augmentedGraph;
    }

    /**
     * Returns the shared distance calculator for the augmented graph, created lazily, mirroring
     * {@link #getDistanceCalculator()}.
     * @return the distance calculator for the augmented graph.
     */
    public DistanceCalculator<String> getAugmentedDistanceCalculator()
    {
        if (augmentedDistanceCalculator == null)
        {
            augmentedDistanceCalculator = new CompleteDistanceCalculator<>();
        }
        return augmentedDistanceCalculator;
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
        this.recommendations.clear();
        this.activeRecommendation = null;
        this.augmentedGraph = null;
        this.augmentedDistanceCalculator = null;
        this.diffusion = null;
    }
}
