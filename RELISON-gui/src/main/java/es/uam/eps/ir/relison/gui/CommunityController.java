/*
 *  Copyright (C) 2023 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.gui;

import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.grid.community.CommunityDetectionSelector;
import es.uam.eps.ir.relison.grid.sna.comm.global.GlobalCommunityMetricIdentifiers;
import es.uam.eps.ir.relison.grid.sna.comm.global.GlobalCommunityMetricSelector;
import es.uam.eps.ir.relison.grid.sna.comm.indiv.IndividualCommunityMetricSelector;
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.sna.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.relison.sna.metrics.CommunityMetric;
import es.uam.eps.ir.relison.sna.metrics.IndividualCommunityMetric;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * REST handlers for detecting communities and computing community-level metrics, reusing RELISON's
 * {@code CommunityDetectionSelector} and {@code GlobalCommunityMetricSelector}.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class CommunityController
{
    private final GraphStore store;
    /** Temporary directory required by some detection algorithms (e.g. Infomap). */
    private final String tempFolder = System.getProperty("java.io.tmpdir");

    /**
     * Global community metrics exposed by the GUI, each carrying the default parameters used to compute it. They are
     * all computed together over the stored partition; the degree/edge Gini variants need an orientation and/or a
     * self-loop flag, so each definition provides sensible defaults via {@link Grids#build(List, Map)}.
     */
    private static final List<MetricCatalog.MetricDef> GLOBAL_METRICS = new ArrayList<>();

    static
    {
        // Parameter-free summaries.
        global(GlobalCommunityMetricIdentifiers.NUMCOMMS, "Number of communities");
        global(GlobalCommunityMetricIdentifiers.MODULARITY, "Modularity");
        global(GlobalCommunityMetricIdentifiers.MODULARITYCOMPL, "Modularity complement");
        global(GlobalCommunityMetricIdentifiers.COMMSIZEGINI, "Community size Gini");
        global(GlobalCommunityMetricIdentifiers.COMMDESTSIZE, "Destination community size");
        global(GlobalCommunityMetricIdentifiers.WEAKTIES, "Weak ties");

        // Degree Gini (need an orientation; the "complete" variants also count node self-loops).
        global(GlobalCommunityMetricIdentifiers.INTERCOMMUNITYDEGREEGINI, "Inter-community degree Gini", orientation());
        global(GlobalCommunityMetricIdentifiers.SIZENORMINTERCOMMUNITYDEGREEGINI, "Size-normalized inter-community degree Gini", orientation());
        global(GlobalCommunityMetricIdentifiers.COMPLETECOMMUNITYDEGREEGINI, "Complete community degree Gini", orientation(), autoloops());
        global(GlobalCommunityMetricIdentifiers.SIZENORMCOMPLETECOMMUNITYDEGREEGINI, "Size-normalized complete community degree Gini", orientation(), autoloops());

        // Edge Gini (the "complete"/"semi-complete" variants optionally count self-loops).
        global(GlobalCommunityMetricIdentifiers.INTERCOMMUNITYEDGEGINI, "Inter-community edge Gini complement");
        global(GlobalCommunityMetricIdentifiers.COMPLETECOMMUNITYEDGEGINI, "Complete community edge Gini complement", selfloops());
        global(GlobalCommunityMetricIdentifiers.SEMICOMPLETECOMMUNITYEDGEGINI, "Semi-complete community edge Gini complement", selfloops());
        global(GlobalCommunityMetricIdentifiers.SIZENORMINTERCOMMUNITYEDGEGINI, "Size-normalized inter-community edge Gini");
        global(GlobalCommunityMetricIdentifiers.SIZENORMCOMPLETECOMMUNITYEDGEGINI, "Size-normalized complete community edge Gini", autoloops());
        global(GlobalCommunityMetricIdentifiers.SIZENORMSEMICOMPLETECOMMUNITYEDGEGINI, "Size-normalized semi-complete community edge Gini", autoloops());
        global(GlobalCommunityMetricIdentifiers.DICEINTERCOMMUNITYEDGEGINI, "Dice inter-community edge Gini");
        global(GlobalCommunityMetricIdentifiers.DICECOMPLETECOMMUNITYEDGEGINI, "Dice complete community edge Gini", autoloops());
        global(GlobalCommunityMetricIdentifiers.DICESEMICOMPLETECOMMUNITYEDGEGINI, "Dice semi-complete community edge Gini", autoloops());
    }

    private static void global(String id, String label, Param... params)
    {
        GLOBAL_METRICS.add(new MetricCatalog.MetricDef(id, label, List.of(params)));
    }

    private static Param orientation()
    {
        return Param.orientation("orientation", "Orientation", "OUT");
    }

    private static Param autoloops()
    {
        return Param.bool("autoloops", "Count self-loops", true);
    }

    private static Param selfloops()
    {
        return Param.bool("selfloops", "Count self-loops", true);
    }

    public CommunityController(GraphStore store)
    {
        this.store = store;
    }

    /**
     * Handles {@code POST /api/communities}: runs a detection algorithm, stores the partition in the session, and
     * returns the per-node community assignment.
     * @param ctx the request context, with body {@code {graphId, algorithm}}.
     */
    public void detect(Context ctx)
    {
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        GraphSession session = requireSession(ctx, body);
        if (session == null) return;

        String algorithmId = String.valueOf(body.get("algorithm"));
        CommunityCatalog.AlgorithmDef def = CommunityCatalog.algorithms().get(algorithmId);
        if (def == null)
        {
            ctx.status(400).json(Map.of("error", "Unknown community detection algorithm: " + algorithmId));
            return;
        }

        CommunityDetectionSelector<String> selector = new CommunityDetectionSelector<>(tempFolder);
        Grid grid = Grids.build(def.params, MetricController.paramsOf(body));
        Map<String, Supplier<CommunityDetectionAlgorithm<String>>> algorithms =
                selector.getCommunityDetectionAlgorithms(algorithmId, grid);
        if (algorithms == null || algorithms.isEmpty())
        {
            ctx.status(400).json(Map.of("error", "Algorithm " + algorithmId + " could not be configured."));
            return;
        }

        Communities<String> communities;
        try
        {
            CommunityDetectionAlgorithm<String> algorithm = algorithms.values().iterator().next().get();
            communities = algorithm.detectCommunities(session.getGraph());
        }
        catch (Exception | UnsatisfiedLinkError | NoClassDefFoundError e)
        {
            ctx.status(500).json(Map.of("error", "Detection failed (" + algorithmId
                    + "). This algorithm may need an external tool or extra libraries: " + e));
            return;
        }

        // Key by algorithm + parameters so the same algorithm with different parameters yields distinct partitions.
        String key = def.label + Grids.suffix(def.params, MetricController.paramsOf(body));
        session.getCommunities().put(key, communities);

        Map<String, Integer> assignment = new LinkedHashMap<>();
        session.getGraph().getAllNodes().forEach(node ->
                assignment.put(node, communities.getCommunity(node)));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("algorithm", key);
        response.put("label", key);
        response.put("numCommunities", communities.getNumCommunities());
        response.put("values", assignment);
        ctx.json(response);
    }

    /**
     * Handles {@code POST /api/communities/metrics}: computes the curated global community metrics over a stored
     * partition.
     * @param ctx the request context, with body {@code {graphId, algorithm}} (the partition to evaluate).
     */
    public void metrics(Context ctx)
    {
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        GraphSession session = requireSession(ctx, body);
        if (session == null) return;

        String algorithmId = String.valueOf(body.get("algorithm"));
        Communities<String> communities = session.getCommunities().get(algorithmId);
        if (communities == null)
        {
            ctx.status(400).json(Map.of("error", "No detected partition for " + algorithmId + ". Run detection first."));
            return;
        }

        GlobalCommunityMetricSelector<String> selector = new GlobalCommunityMetricSelector<>();
        List<Map<String, Object>> results = new ArrayList<>();
        for (MetricCatalog.MetricDef def : GLOBAL_METRICS)
        {
            Grid grid = Grids.build(def.params, null); // all-defaults grid for this metric
            Map<String, Supplier<CommunityMetric<String>>> metrics = selector.getMetrics(def.id, grid);
            if (metrics == null || metrics.isEmpty()) continue;
            try
            {
                CommunityMetric<String> metric = metrics.values().iterator().next().get();
                double value = metric.compute(session.getGraph(), communities);
                results.add(Map.of("metric", def.id, "label", def.label, "value", value));
            }
            catch (Exception e)
            {
                results.add(Map.of("metric", def.id, "label", def.label, "error", String.valueOf(e)));
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("algorithm", algorithmId);
        response.put("numCommunities", communities.getNumCommunities());
        response.put("metrics", results);
        ctx.json(response);
    }

    /**
     * Handles {@code POST /api/communities/individual}: computes a per-community metric over a stored partition.
     * @param ctx the request context, with body {@code {graphId, algorithm, metric, params}}.
     */
    public void individual(Context ctx)
    {
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        GraphSession session = requireSession(ctx, body);
        if (session == null) return;

        String algorithm = String.valueOf(body.get("algorithm"));
        Communities<String> communities = session.getCommunities().get(algorithm);
        if (communities == null)
        {
            ctx.status(400).json(Map.of("error", "No detected partition for " + algorithm + ". Run detection first."));
            return;
        }

        String metricId = String.valueOf(body.get("metric"));
        MetricCatalog.MetricDef def = CommunityCatalog.individualMetrics().get(metricId);
        if (def == null)
        {
            ctx.status(400).json(Map.of("error", "Unknown individual community metric: " + metricId));
            return;
        }

        Grid grid = Grids.build(def.params, MetricController.paramsOf(body));
        IndividualCommunityMetricSelector<String> selector = new IndividualCommunityMetricSelector<>();
        Map<String, Supplier<IndividualCommunityMetric<String>>> metrics = selector.getMetrics(metricId, grid);
        if (metrics == null || metrics.isEmpty())
        {
            ctx.status(400).json(Map.of("error", "Metric " + metricId + " could not be configured."));
            return;
        }

        IndividualCommunityMetric<String> metric = metrics.values().iterator().next().get();
        Map<Integer, Double> values = metric.compute(session.getGraph(), communities);

        Map<String, Double> out = new LinkedHashMap<>();
        values.forEach((comm, value) -> out.put(Integer.toString(comm), value));
        double average = values.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("metric", metricId);
        response.put("label", def.label + Grids.suffix(def.params, MetricController.paramsOf(body)));
        response.put("algorithm", algorithm);
        response.put("values", out);
        response.put("average", average);
        ctx.json(response);
    }

    private GraphSession requireSession(Context ctx, Map<?, ?> body)
    {
        Object graphId = body.get("graphId");
        GraphSession session = graphId == null ? null : store.get(String.valueOf(graphId));
        if (session == null)
        {
            ctx.status(404).json(Map.of("error", "Unknown graph id."));
            return null;
        }
        return session;
    }
}
