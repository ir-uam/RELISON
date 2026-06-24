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

    /** Curated set of global community metrics exposed by the GUI, as {identifier -> label}. */
    private static final Map<String, String> GLOBAL_METRICS = new LinkedHashMap<>();

    static
    {
        GLOBAL_METRICS.put(GlobalCommunityMetricIdentifiers.NUMCOMMS, "Number of communities");
        GLOBAL_METRICS.put(GlobalCommunityMetricIdentifiers.MODULARITY, "Modularity");
        GLOBAL_METRICS.put(GlobalCommunityMetricIdentifiers.COMMSIZEGINI, "Community size Gini");
        GLOBAL_METRICS.put(GlobalCommunityMetricIdentifiers.WEAKTIES, "Weak ties");
        GLOBAL_METRICS.put(GlobalCommunityMetricIdentifiers.INTERCOMMUNITYEDGEGINI, "Inter-community edge Gini complement");
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
        List<Map<String, Object>> results = new java.util.ArrayList<>();
        for (Map.Entry<String, String> entry : GLOBAL_METRICS.entrySet())
        {
            Map<String, Supplier<CommunityMetric<String>>> metrics =
                    selector.getMetrics(entry.getKey(), new Grid());
            if (metrics == null || metrics.isEmpty()) continue;
            try
            {
                CommunityMetric<String> metric = metrics.values().iterator().next().get();
                double value = metric.compute(session.getGraph(), communities);
                results.add(Map.of("metric", entry.getKey(), "label", entry.getValue(), "value", value));
            }
            catch (Exception e)
            {
                results.add(Map.of("metric", entry.getKey(), "label", entry.getValue(), "error", String.valueOf(e)));
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
