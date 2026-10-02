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
import es.uam.eps.ir.relison.grid.sna.comm.global.GlobalCommunityMetricSelector;
import es.uam.eps.ir.relison.grid.sna.comm.indiv.IndividualCommunityMetricSelector;
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.sna.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.relison.sna.metrics.CommunityMetric;
import es.uam.eps.ir.relison.sna.metrics.IndividualCommunityMetric;
import es.uam.eps.ir.relison.sna.metrics.NodeAttributeMetric;
import es.uam.eps.ir.relison.sna.metrics.IndividualNodeAttributeMetric;
import es.uam.eps.ir.relison.sna.metrics.attributes.nodes.graph.CommunityBasedNodeAttributeMetric;
import es.uam.eps.ir.relison.sna.metrics.attributes.nodes.indiv.CommunityBasedIndividualNodeAttributeMetric;
import io.javalin.http.Context;

import java.util.LinkedHashMap;
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
    private final JobManager jobs;
    /** Temporary directory required by some detection algorithms (e.g. Infomap). */
    private final String tempFolder = System.getProperty("java.io.tmpdir");

    public CommunityController(GraphStore store, JobManager jobs)
    {
        this.store = store;
        this.jobs = jobs;
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

        CommunityDetectionAlgorithm<String> algorithm = algorithms.values().iterator().next().get();
        Communities<String> communities;
        try
        {
            communities = jobs.run(MetricController.jobKey(body, "community"),
                    () -> algorithm.detectCommunities(session.getGraph()));
        }
        catch (JobCancelledException e)
        {
            ctx.json(Map.of("cancelled", true));
            return;
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
     * Handles {@code POST /api/communities/global}: computes a single global community metric over a stored partition,
     * with its own parameters and optionally over the recommendation-augmented graph.
     * @param ctx the request context, with body {@code {graphId, algorithm, metric, params, withRecommendation?}}.
     */
    public void globalMetric(Context ctx)
    {
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        GraphSession session = requireSession(ctx, body);
        if (session == null) return;

        String metricId = String.valueOf(body.get("metric"));
        MetricCatalog.MetricDef def = CommunityCatalog.globalMetrics().get(metricId);
        if (def == null) { ctx.status(400).json(Map.of("error", "Unknown global community metric: " + metricId)); return; }

        Grid grid = Grids.build(def.params, MetricController.paramsOf(body));
        GlobalCommunityMetricSelector<String> selector = new GlobalCommunityMetricSelector<>();
        Map<String, Supplier<CommunityMetric<String>>> metrics = selector.getMetrics(metricId, grid);
        if (metrics == null || metrics.isEmpty()) { ctx.status(400).json(Map.of("error", "Metric " + metricId + " could not be configured.")); return; }

        String attribute = attributeOf(body);
        String algorithm = attribute == null ? String.valueOf(body.get("algorithm")) : "Attribute: " + attribute;
        Communities<String> communities = attribute == null ? session.getCommunities().get(algorithm) : null;
        if (attribute == null && communities == null) { ctx.status(400).json(Map.of("error", "No detected partition for " + algorithm + ". Run detection first.")); return; }
        if (attribute != null && session.getGraph().getNodeAttributeType(attribute) == null) { ctx.status(400).json(Map.of("error", "Unknown node attribute: " + attribute)); return; }

        boolean withRec = MetricController.useRecommendation(body, session);
        Graph<String> graph = withRec ? session.getAugmentedGraph() : session.getGraph();
        CommunityMetric<String> metric = metrics.values().iterator().next().get();
        double value;
        try
        {
            if (attribute == null) value = jobs.run(MetricController.jobKey(body, "commGlobal"), () -> metric.compute(graph, communities));
            else
            {
                NodeAttributeMetric<String> attributeMetric = new CommunityBasedNodeAttributeMetric<>(metric);
                value = jobs.run(MetricController.jobKey(body, "commGlobal"), () -> attributeMetric.compute(graph, attribute));
            }
        }
        catch (JobCancelledException e) { ctx.json(Map.of("cancelled", true)); return; }
        catch (Exception e) { ctx.status(500).json(Map.of("error", String.valueOf(e))); return; }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("metric", metricId);
        response.put("label", def.label + Grids.suffix(def.params, MetricController.paramsOf(body)));
        response.put("algorithm", algorithm);
        response.put("value", value);
        response.put("recommendation", withRec ? session.getActiveRecommendationKey() : null);
        ctx.json(response);
    }

    /** Computes a per-community metric, or its shared RELISON-SNA attribute-based equivalent. */
    public void individual(Context ctx)
    {
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        GraphSession session = requireSession(ctx, body);
        if (session == null) return;

        String metricId = String.valueOf(body.get("metric"));
        MetricCatalog.MetricDef def = CommunityCatalog.individualMetrics().get(metricId);
        if (def == null) { ctx.status(400).json(Map.of("error", "Unknown individual community metric: " + metricId)); return; }

        Grid grid = Grids.build(def.params, MetricController.paramsOf(body));
        IndividualCommunityMetricSelector<String> selector = new IndividualCommunityMetricSelector<>();
        Map<String, Supplier<IndividualCommunityMetric<String>>> metrics = selector.getMetrics(metricId, grid);
        if (metrics == null || metrics.isEmpty()) { ctx.status(400).json(Map.of("error", "Metric " + metricId + " could not be configured.")); return; }

        String attribute = attributeOf(body);
        String algorithm = attribute == null ? String.valueOf(body.get("algorithm")) : "Attribute: " + attribute;
        Communities<String> communities = attribute == null ? session.getCommunities().get(algorithm) : null;
        if (attribute == null && communities == null) { ctx.status(400).json(Map.of("error", "No detected partition for " + algorithm + ". Run detection first.")); return; }
        if (attribute != null && session.getGraph().getNodeAttributeType(attribute) == null) { ctx.status(400).json(Map.of("error", "Unknown node attribute: " + attribute)); return; }

        boolean withRec = MetricController.useRecommendation(body, session);
        Graph<String> graph = withRec ? session.getAugmentedGraph() : session.getGraph();
        IndividualCommunityMetric<String> metric = metrics.values().iterator().next().get();
        Map<String, Double> out = new LinkedHashMap<>();
        Map<String, String> valueLabels = new LinkedHashMap<>();
        try
        {
            if (attribute == null)
            {
                Map<Integer, Double> values = jobs.run(MetricController.jobKey(body, "commIndividual"), () -> metric.compute(graph, communities));
                values.forEach((community, value) -> out.put(Integer.toString(community), value));
            }
            else
            {
                IndividualNodeAttributeMetric<String> attributeMetric = new CommunityBasedIndividualNodeAttributeMetric<>(metric);
                Map<Object, Double> values = jobs.run(MetricController.jobKey(body, "commIndividual"), () -> attributeMetric.compute(graph, attribute));
                int index = 0;
                for (Map.Entry<Object, Double> entry : values.entrySet())
                {
                    String id = Integer.toString(index++);
                    out.put(id, entry.getValue());
                    valueLabels.put(id, entry.getKey() == null ? "(missing)" : String.valueOf(entry.getKey()));
                }
            }
        }
        catch (JobCancelledException e) { ctx.json(Map.of("cancelled", true)); return; }
        catch (Exception e) { ctx.status(500).json(Map.of("error", String.valueOf(e))); return; }

        double average = out.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("metric", metricId);
        response.put("label", def.label + Grids.suffix(def.params, MetricController.paramsOf(body)));
        response.put("algorithm", algorithm);
        response.put("values", out);
        response.put("valueLabels", valueLabels);
        response.put("average", average);
        response.put("recommendation", withRec ? session.getActiveRecommendationKey() : null);
        ctx.json(response);
    }

    private static String attributeOf(Map<?, ?> body)
    {
        Object value = body.get("attribute");
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value);
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
