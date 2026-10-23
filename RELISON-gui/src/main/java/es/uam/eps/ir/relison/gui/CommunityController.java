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
import es.uam.eps.ir.relison.grid.sna.graph.GraphMetricSelector;
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.sna.community.detection.attributes.AttributeCommunityDetector;
import es.uam.eps.ir.relison.sna.community.detection.attributes.NumericAttributeCommunityDetector;
import es.uam.eps.ir.relison.sna.community.detection.attributes.AttributePartition;
import es.uam.eps.ir.relison.sna.community.graph.SimpleCommunityGraphGenerator;
import es.uam.eps.ir.relison.sna.community.graph.CompleteCommunityGraphGenerator;
import es.uam.eps.ir.relison.sna.community.graph.InterCommunityGraphGenerator;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.fast.FastDirectedWeightedGraph;
import es.uam.eps.ir.relison.graph.fast.FastUndirectedWeightedGraph;
import es.uam.eps.ir.relison.graph.multigraph.MultiGraph;
import es.uam.eps.ir.relison.sna.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.relison.sna.metrics.CommunityMetric;
import es.uam.eps.ir.relison.sna.metrics.IndividualCommunityMetric;
import es.uam.eps.ir.relison.sna.metrics.NodeAttributeMetric;
import es.uam.eps.ir.relison.sna.metrics.IndividualNodeAttributeMetric;
import es.uam.eps.ir.relison.sna.metrics.GraphMetric;
import es.uam.eps.ir.relison.sna.metrics.distance.CompleteDistanceCalculator;
import es.uam.eps.ir.relison.sna.metrics.communities.indiv.IndividualCommunityGraphMetric;
import es.uam.eps.ir.relison.sna.metrics.attributes.nodes.graph.CommunityBasedNodeAttributeMetric;
import es.uam.eps.ir.relison.sna.metrics.attributes.nodes.indiv.CommunityBasedIndividualNodeAttributeMetric;
import io.javalin.http.Context;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Supplier;

/**
 * REST handlers for detecting communities and computing community-level metrics, reusing RELISON's
 * {@code CommunityDetectionSelector} and {@code GlobalCommunityMetricSelector}.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class CommunityController
{
    static final List<String> GRAPH_LAYOUTS = List.of("circular", "fruchterman-reingold", "grid", "random", "spectral",
            "stress-majorization", "kamada-kawai", "multilevel-force", "relison-forceatlas2", "concentric",
            "spherical-3d", "fruchterman-reingold-3d", "grid-3d", "random-3d", "spectral-3d", "stress-majorization-3d",
            "kamada-kawai-flat-3d", "multilevel-force-flat-3d", "relison-forceatlas2-flat-3d", "concentric-sphere-3d");
    private final GraphStore store;
    private final JobManager jobs;
    /** Temporary directory required by some detection algorithms (e.g. Infomap). */
    private final String tempFolder = System.getProperty("java.io.tmpdir");

    public CommunityController(GraphStore store, JobManager jobs)
    {
        this.store = store;
        this.jobs = jobs;
    }

    /** Community graph for a stored partition. Display aggregation never changes the source network. */
    public void graph(Context ctx)
    {
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        GraphSession session = requireSession(ctx, body);
        if (session == null) return;
        String attribute = attributeOf(body);
        Communities<String> partition;
        if (attribute != null)
        {
            if (session.getGraph().getNodeAttributeType(attribute) == null)
            {
                ctx.status(400).json(Map.of("error", "Unknown node attribute: " + attribute));
                return;
            }
            partition = new AttributeCommunityDetector<String>(attribute).detectCommunities(session.getGraph());
        }
        else partition = session.getCommunities().get(String.valueOf(body.get("algorithm")));
        if (partition == null)
        {
            ctx.status(400).json(Map.of("error", "Partition is no longer available. Run detection again."));
            return;
        }
        try
        {
            ctx.json(communityGraph(session.getGraph(), partition, body));
        }
        catch (IllegalArgumentException e)
        {
            ctx.status(400).json(Map.of("error", e.getMessage()));
        }
    }

    /** JSON contract kept independent of HTTP so generator semantics can be verified directly. */
    static Map<String, Object> communityGraph(Graph<String> graph, Communities<String> partition, Map<?, ?> body)
    {
        graph.getAllNodes().forEach(node -> {
            if (partition.getCommunity(node) < 0)
                throw new IllegalArgumentException("Partition does not cover the current network. Run detection again.");
        });
        String mode = body.containsKey("mode") ? String.valueOf(body.get("mode")) : "weighted";
        String layout = body.containsKey("layout") ? String.valueOf(body.get("layout")) : "circular";
        if (!GRAPH_LAYOUTS.contains(layout))
            throw new IllegalArgumentException("Unknown community graph layout: " + layout);
        Graph<Integer> communityGraph;
        if ("weighted".equals(mode))
        {
            SimpleCommunityGraphGenerator<String> generator = new SimpleCommunityGraphGenerator<>();
            generator.configure(graph, partition, true);
            try { communityGraph = generator.generate(); }
            catch (Exception e) { throw new IllegalArgumentException("Community graph generation failed: " + e.getMessage(), e); }
        }
        else if ("complete".equals(mode)) communityGraph = new CompleteCommunityGraphGenerator<String>().generate(graph, partition);
        else if ("inter".equals(mode)) communityGraph = new InterCommunityGraphGenerator<String>().generate(graph, partition);
        else throw new IllegalArgumentException("Unknown community graph mode: " + mode);
        if (communityGraph == null) throw new IllegalArgumentException("Community graph generation failed.");

        Graph<String> display = graph.isDirected() ? new FastDirectedWeightedGraph<>() : new FastUndirectedWeightedGraph<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        partition.getCommunities().forEach(id -> {
            display.addNode(Integer.toString(id));
            nodes.add(Map.of("id", Integer.toString(id), "size", partition.getCommunitySize(id)));
        });
        final Graph<Integer> generated = communityGraph;
        generated.getAllNodes().forEach(source -> generated.getAdjacentNodes(source).forEach(target -> {
            if (!generated.isDirected() && source > target) return;
            // A single visible link per pair; keep the exact RELISON generator's multiplicity as its value.
            double value = generated instanceof MultiGraph
                    ? ((MultiGraph<Integer>) generated).getNumEdges(source, target)
                    : generated.getEdgeWeight(source, target);
            if (!Double.isFinite(value)) throw new IllegalArgumentException("Community graph contains a non-finite connection weight.");
            edges.add(Map.of("source", source.toString(), "target", target.toString(), "weight", value));
            display.addEdge(source.toString(), target.toString(), Math.abs(value));
        }));
        Map<String, Object> positions = LayoutController.compute(display, Map.of("algorithm", layout,
                "params", Map.of("iterations", 250, "seed", 42)));
        return Map.of("nodes", nodes, "edges", edges, "directed", graph.isDirected(), "mode", mode,
                "positions", positions.get("positions"));
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

        if (communities == null) { ctx.status(500).json(Map.of("error", "Community detection returned no partition.")); return; }

        // Key by algorithm + parameters so the same algorithm with different parameters yields distinct partitions.
        String key = def.label + Grids.suffix(def.params, MetricController.paramsOf(body));

        Map<String, Integer> assignment = new LinkedHashMap<>();
        session.getGraph().getAllNodes().forEach(node ->
                assignment.put(node, communities.getCommunity(node)));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("algorithm", key);
        response.put("label", key);
        response.put("numCommunities", communities.getNumCommunities());
        response.put("values", assignment);
        ctx.json(response);
        JobManager.publish(() -> session.getCommunities().put(key, communities));
    }

    /** Detects node communities by fixed-width bins over a numeric node attribute. */
    public void detectAttributeBins(Context ctx)
    {
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        GraphSession session = requireSession(ctx, body);
        if (session == null) return;
        Object rawAttribute = body.get("attribute"), rawWidth = body.get("binWidth");
        if (rawAttribute == null || rawAttribute.toString().trim().isEmpty() || rawWidth == null)
        {
            ctx.status(400).json(Map.of("error", "Choose a numeric node attribute and bin width."));
            return;
        }
        String attribute = rawAttribute.toString();
        double binWidth;
        try { binWidth = Double.parseDouble(rawWidth.toString()); }
        catch (NumberFormatException e) { ctx.status(400).json(Map.of("error", "Bin width must be numeric.")); return; }

        NumericAttributeCommunityDetector<String> detector;
        try { detector = new NumericAttributeCommunityDetector<>(attribute, binWidth); }
        catch (IllegalArgumentException e) { ctx.status(400).json(Map.of("error", e.getMessage())); return; }
        AttributePartition<String, Object> attributePartition;
        try
        {
            attributePartition = jobs.run(MetricController.jobKey(body, "community"),
                    () -> detector.detectAttributePartition(session.getGraph()));
        }
        catch (JobCancelledException e) { ctx.json(Map.of("cancelled", true)); return; }
        catch (IllegalArgumentException e) { ctx.status(400).json(Map.of("error", e.getMessage())); return; }
        catch (Exception e) { ctx.status(500).json(Map.of("error", "Numeric attribute partition failed: " + e.getMessage())); return; }

        Communities<String> communities = attributePartition.getCommunities();
        String key = "Attribute bins: " + attribute + " (width " + binWidth + ")";
        session.getCommunities().put(key, communities);
        Map<String, Integer> assignment = new LinkedHashMap<>();
        session.getGraph().getAllNodes().forEach(node -> assignment.put(node, communities.getCommunity(node)));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("algorithm", key);
        response.put("label", key);
        response.put("numCommunities", communities.getNumCommunities());
        response.put("values", assignment);
        response.put("attribute", attribute);
        response.put("binWidth", binWidth);
        Map<Integer, String> communityLabels = new LinkedHashMap<>();
        attributePartition.getMapping().forEach((id, value) -> communityLabels.put(id,
                value == null ? "Unknown" : String.valueOf(value)));
        response.put("communityLabels", communityLabels);
        ctx.json(response);
        JobManager.publish(() -> session.getCommunities().put(key, communities));
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
        boolean intraCommunity = metricId.startsWith("intra-community:");
        boolean intraAttribute = metricId.startsWith("intra-attribute:");
        String graphMetricId = intraCommunity ? metricId.substring("intra-community:".length())
                : intraAttribute ? metricId.substring("intra-attribute:".length()) : null;
        MetricCatalog.MetricDef def = graphMetricId == null ? CommunityCatalog.individualMetrics().get(metricId)
                : MetricCatalog.graphMetrics().get(graphMetricId);
        if (def == null) { ctx.status(400).json(Map.of("error", "Unknown individual community metric: " + metricId)); return; }

        IndividualCommunityMetric<String> metric;
        try
        {
            Grid grid = Grids.build(def.params, MetricController.paramsOf(body));
            if (graphMetricId == null)
            {
                IndividualCommunityMetricSelector<String> selector = new IndividualCommunityMetricSelector<>();
                Map<String, Supplier<IndividualCommunityMetric<String>>> metrics = selector.getMetrics(metricId, grid);
                if (metrics == null || metrics.isEmpty()) { ctx.status(400).json(Map.of("error", "Metric " + metricId + " could not be configured.")); return; }
                metric = metrics.values().iterator().next().get();
            }
            else
            {
                GraphMetricSelector<String> selector = new GraphMetricSelector<>();
                Map<String, Supplier<GraphMetric<String>>> metrics = selector.getMetrics(graphMetricId, grid,
                        new CompleteDistanceCalculator<>());
                if (metrics == null || metrics.isEmpty()) { ctx.status(400).json(Map.of("error", "Metric " + graphMetricId + " could not be configured.")); return; }
                metric = new IndividualCommunityGraphMetric<>(() -> {
                    Map<String, Supplier<GraphMetric<String>>> perGroup = new GraphMetricSelector<String>()
                            .getMetrics(graphMetricId, grid, new CompleteDistanceCalculator<>());
                    return perGroup.values().iterator().next().get();
                });
            }
        }
        catch (IllegalArgumentException e) { ctx.status(400).json(Map.of("error", e.getMessage())); return; }

        String attribute = attributeOf(body);
        String algorithm = attribute == null ? String.valueOf(body.get("algorithm")) : "Attribute: " + attribute;
        Communities<String> communities = attribute == null ? session.getCommunities().get(algorithm) : null;
        if (attribute == null && communities == null) { ctx.status(400).json(Map.of("error", "No detected partition for " + algorithm + ". Run detection first.")); return; }
        if (attribute != null && session.getGraph().getNodeAttributeType(attribute) == null) { ctx.status(400).json(Map.of("error", "Unknown node attribute: " + attribute)); return; }

        boolean withRec = MetricController.useRecommendation(body, session);
        Graph<String> graph = withRec ? session.getAugmentedGraph() : session.getGraph();
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
        catch (IllegalArgumentException e) { ctx.status(400).json(Map.of("error", e.getMessage())); return; }
        catch (Exception e) { ctx.status(500).json(Map.of("error", String.valueOf(e))); return; }

        double average = out.values().stream().mapToDouble(Double::doubleValue)
                .filter(Double::isFinite).average().orElse(Double.NaN);
        // Undefined boundary ratios are JSON nulls, not strings such as "NaN".
        // The frontend preserves them as missing values and excludes them from averages.
        out.replaceAll((id, value) -> Double.isFinite(value) ? value : null);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("metric", metricId);
        String prefix = intraCommunity ? "Intra-community " : intraAttribute ? "Intra-Attribute " : "";
        response.put("label", prefix + def.label + Grids.suffix(def.params, MetricController.paramsOf(body)));
        response.put("algorithm", algorithm);
        response.put("values", out);
        response.put("valueLabels", valueLabels);
        response.put("average", Double.isFinite(average) ? average : null);
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
        return MetricController.metricSession(ctx, body, session);
    }
}
