package es.uam.eps.ir.relison.gui;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.attributes.AttributeType;
import es.uam.eps.ir.relison.graph.multigraph.MultiGraph;
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.sna.community.detection.attributes.AttributeCommunityDetector;
import es.uam.eps.ir.relison.sna.edge.Edge;
import es.uam.eps.ir.relison.sna.edge.EdgeAttributePartition;
import es.uam.eps.ir.relison.sna.edge.EdgePartition;
import es.uam.eps.ir.relison.sna.edge.NodeAttributeRelationPartition;
import es.uam.eps.ir.relison.sna.edge.detection.attributes.EdgeAttributePartitionDetector;
import es.uam.eps.ir.relison.sna.edge.detection.attributes.NodeAttributeRelationEdgePartitionDetector;
import es.uam.eps.ir.relison.sna.edge.detection.community.CommunityInternalExternalEdgePartitionDetector;
import es.uam.eps.ir.relison.sna.edge.detection.community.CommunityRelationEdgePartitionDetector;
import es.uam.eps.ir.relison.sna.edge.detection.EdgePartitionSupport;
import io.javalin.http.Context;
import es.uam.eps.ir.relison.sna.metrics.IndividualEdgePartitionMetric;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** REST endpoint for creating inspectable edge partitions. */
public class EdgePartitionController
{
    private final GraphStore store;
    private final JobManager jobs;

    public EdgePartitionController(GraphStore store, JobManager jobs)
    {
        this.store = store;
        this.jobs = jobs;
    }

    public void detect(Context ctx)
    {
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        GraphSession session = store.get(String.valueOf(body.get("graphId")));
        if (session == null)
        {
            ctx.status(404).json(Map.of("error", "Unknown graph id."));
            return;
        }

        Graph<String> graph = session.getGraph();
        String type = String.valueOf(body.get("type"));
        EdgePartition<String> partition;
        Map<Integer, String> groupLabels = new LinkedHashMap<>();
        try
        {
            if ("edge-attribute".equals(type))
            {
                String attribute = requiredString(body, "attribute");
                Double binWidth = optionalDouble(body, "binWidth");
                AttributeType attributeType = graph.getEdgeAttributeType(attribute);
                if (attributeType != null && attributeType.isNumeric() && binWidth == null)
                    throw new IllegalArgumentException("Choose a bin width for the numeric edge attribute.");
                EdgeAttributePartition<String, Object> result = new EdgeAttributePartitionDetector<String>(attribute, binWidth)
                        .detectEdgePartition(graph);
                partition = result;
                result.getPartitions().forEach(group -> groupLabels.put(group,
                        display(result.getAttributeValue(group))));
            }
            else if ("node-attribute-relation".equals(type))
            {
                String attribute = requiredString(body, "attribute");
                Double binWidth = optionalDouble(body, "binWidth");
                NodeAttributeRelationPartition<String> result = new NodeAttributeRelationEdgePartitionDetector<String>(attribute, binWidth)
                        .detectEdgePartition(graph);
                partition = result;
                result.getPartitions().forEach(group -> groupLabels.put(group, result.getRelationLabel(group)));
            }
            else if ("community-internal-external".equals(type) || "community-relations".equals(type))
            {
                Communities<String> communities = getCommunities(session, body);
                if (communities == null)
                {
                    ctx.status(400).json(Map.of("error", "Select an available community partition first."));
                    return;
                }
                if ("community-internal-external".equals(type))
                {
                    partition = new CommunityInternalExternalEdgePartitionDetector<>(communities).detectEdgePartition(graph);
                    groupLabels.put(CommunityInternalExternalEdgePartitionDetector.INTRACOMMUNITY, "Within community");
                    groupLabels.put(CommunityInternalExternalEdgePartitionDetector.INTERCOMMUNITY, "Between communities");
                    groupLabels.put(CommunityInternalExternalEdgePartitionDetector.UNASSIGNED, "Unassigned endpoint");
                }
                else
                {
                    partition = new CommunityRelationEdgePartitionDetector<>(communities).detectEdgePartition(graph);
                    EdgePartition<String> detected = partition;
                    EdgePartitionSupport.forEachEdge(graph, edge ->
                    {
                        int group = detected.getPartition(edge);
                        if (group < 0 || groupLabels.containsKey(group)) return;
                        int source = communities.getCommunity(edge.getSource());
                        int target = communities.getCommunity(edge.getTarget());
                        if (!graph.isDirected() && source > target) { int swap = source; source = target; target = swap; }
                        groupLabels.put(group, communityName(source) + (graph.isDirected() ? " → " : " ↔ ") + communityName(target));
                    });
                }
            }
            else
            {
                ctx.status(400).json(Map.of("error", "Unknown edge partition type: " + type));
                return;
            }
        }
        catch (IllegalArgumentException e)
        {
            ctx.status(400).json(Map.of("error", e.getMessage()));
            return;
        }

        ctx.json(partitionResponse(session, graph, partition, groupLabels, type, body, java.util.UUID.randomUUID().toString()));
    }

    private Map<String, Object> partitionResponse(GraphSession session, Graph<String> graph, EdgePartition<String> resultPartition,
            Map<Integer, String> groupLabels, String type, Map<?, ?> body, String partitionId)
    {
        Map<Integer, Integer> sizes = new LinkedHashMap<>();
        resultPartition.getPartitions().forEach(group -> sizes.put(group, resultPartition.getPartitionSize(group)));
        List<Map<String, Object>> edges = new ArrayList<>();
        EdgePartitionSupport.forEachEdge(graph, edge ->
        {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("source", edge.getSource());
            row.put("target", edge.getTarget());
            row.put("edgeId", edge.getId() == null ? null : edge.getId().toString());
            row.put("index", edge.getIndex());
            row.put("partition", resultPartition.getPartition(edge));
            edges.add(row);
        });
        List<Map<String, Object>> groups = new ArrayList<>();
        resultPartition.getPartitions().forEach(group -> groups.add(Map.of("id", group,
                "label", groupLabels.getOrDefault(group, "Group " + group), "size", sizes.get(group))));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("type", type);
        response.put("groups", groups);
        response.put("edges", edges);
        response.put("directed", graph.isDirected());
        response.put("multigraph", graph instanceof MultiGraph<?>);
        String label = "edge-attribute".equals(type) ? "Edge attribute: " + body.get("attribute")
                : "node-attribute-relation".equals(type) ? "Endpoint attribute pairs: " + body.get("attribute")
                : "community-internal-external".equals(type) ? "Within / between: " + body.get("partition")
                : "Community pairs: " + body.get("partition");
        if (body.get("binWidth") != null) label += " (bin width " + body.get("binWidth") + ")";
        response.put("partitionId", partitionId);
        response.put("label", label);
        JobManager.publish(() -> session.getEdgePartitions().put(partitionId, resultPartition));
        return response;
    }

    /** Renames a textual edge-attribute category, including every parallel edge in the selected group. */
    public void renameAttributeLabel(Context ctx)
    {
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        GraphSession session = store.get(String.valueOf(body.get("graphId")));
        if (session == null) { ctx.status(404).json(Map.of("error", "Unknown graph id.")); return; }
        String partitionId = String.valueOf(body.get("partitionId"));
        EdgePartition<String> partition = session.getEdgePartitions().get(partitionId);
        if (!(partition instanceof EdgeAttributePartition<?, ?>))
        {
            ctx.status(400).json(Map.of("error", "Select an edge-attribute partition first.")); return;
        }
        Graph<String> graph = session.getGraph();
        try
        {
            String attribute = requiredString(body, "attribute"), label = requiredString(body, "label").trim();
            AttributeType type = graph.getEdgeAttributeType(attribute);
            if (type != AttributeType.STRING && type != AttributeType.CATEGORICAL)
                throw new IllegalArgumentException("Only string and categorical edge-attribute values can be renamed.");
            int group = Integer.parseInt(String.valueOf(body.get("group")));
            if (group < 0 || group >= partition.getNumPartitions())
                throw new IllegalArgumentException("Invalid edge group.");
            if (label.length() > 120) throw new IllegalArgumentException("The label must have at most 120 characters.");
            Object oldValue = ((EdgeAttributePartition<?, ?>) partition).getAttributeValue(group);
            MultiGraph<String> multi = graph instanceof MultiGraph<?> ? (MultiGraph<String>) graph : null;
            List<Edge<String>> selected = new ArrayList<>();
            // Resolve live stable ids once before any writes; a parallel-edge index may have changed.
            EdgePartitionSupport.forEachEdge(graph, edge -> {
                if (partition.getPartition(edge) != group) return;
                Object current = multi == null ? graph.getEdgeAttribute(edge.getSource(), edge.getTarget(), attribute)
                        : multi.getEdgeAttribute(edge.getSource(), edge.getTarget(), edge.getIndex(), attribute);
                if (!java.util.Objects.equals(current, oldValue))
                    throw new IllegalArgumentException("The attribute values changed. Detect the partition again before renaming it.");
                selected.add(edge);
            });
            if (selected.size() != partition.getPartitionSize(group))
                throw new IllegalArgumentException("The graph changed. Detect the partition again before renaming it.");
            // Compare proposed memberships before writing, so the UI can warn and cancel.
            Map<Integer, Object> oldToValue = new LinkedHashMap<>();
            Map<Object, Integer> valueToOld = new LinkedHashMap<>();
            boolean[] membershipChanged = { false };
            EdgePartitionSupport.forEachEdge(graph, edge -> {
                int before = partition.getPartition(edge);
                Object current = multi == null ? graph.getEdgeAttribute(edge.getSource(), edge.getTarget(), attribute)
                        : multi.getEdgeAttribute(edge.getSource(), edge.getTarget(), edge.getIndex(), attribute);
                Object after = before == group ? label : current;
                if (before < 0 || oldToValue.containsKey(before) && !java.util.Objects.equals(oldToValue.get(before), after)
                        || valueToOld.containsKey(after) && valueToOld.get(after) != before) membershipChanged[0] = true;
                oldToValue.put(before, after); valueToOld.put(after, before);
            });
            if (Boolean.TRUE.equals(body.get("preview")))
            {
                Map<String, Object> preview = new LinkedHashMap<>();
                preview.put("membershipChanged", membershipChanged[0]); preview.put("previousValue", oldValue);
                ctx.json(preview); return;
            }
            if (membershipChanged[0] && Boolean.FALSE.equals(body.get("allowMembershipChange")))
                throw new IllegalArgumentException("Category membership changed. Try renaming again to review the affected metrics.");
            int written = 0;
            try
            {
                for (Edge<String> edge : selected)
                {
                    boolean ok = multi == null ? graph.setEdgeAttribute(edge.getSource(), edge.getTarget(), attribute, label)
                            : multi.setEdgeAttribute(edge.getSource(), edge.getTarget(), edge.getIndex(), attribute, label);
                    if (!ok) throw new IllegalArgumentException("Could not update the selected edge attribute.");
                    written++;
                }
                EdgeAttributePartition<String, Object> updated = new EdgeAttributePartitionDetector<String>(attribute).detectEdgePartition(graph);
                Map<Integer, String> labels = new LinkedHashMap<>();
                updated.getPartitions().forEach(id -> labels.put(id, display(updated.getAttributeValue(id))));
                Map<String, Object> response = partitionResponse(session, graph, updated, labels, "edge-attribute",
                        Map.of("attribute", attribute), partitionId);
                response.put("selectedGroup", updated.getPartition(label));
                response.put("membershipChanged", membershipChanged[0]);
                response.put("previousValue", oldValue);
                ctx.json(response);
            }
            catch (RuntimeException error)
            {
                for (int i = 0; i < written; i++)
                {
                    Edge<String> edge = selected.get(i);
                    if (multi == null) graph.setEdgeAttribute(edge.getSource(), edge.getTarget(), attribute, oldValue);
                    else multi.setEdgeAttribute(edge.getSource(), edge.getTarget(), edge.getIndex(), attribute, oldValue);
                }
                session.getEdgePartitions().put(partitionId, partition);
                throw error;
            }
        }
        catch (IllegalArgumentException error) { ctx.status(400).json(Map.of("error", error.getMessage())); }
    }

    public void globalMetric(Context ctx) { metric(ctx, false); }
    public void individualMetric(Context ctx) { metric(ctx, true); }

    private void metric(Context ctx, boolean individual)
    {
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        GraphSession session = MetricController.metricSession(ctx, body, store.get(String.valueOf(body.get("graphId"))));
        if (session == null && body.get("recommendationKey") != null) return;
        if (session == null) { ctx.status(404).json(Map.of("error", "Unknown graph id.")); return; }
        Graph<String> metricGraph = MetricController.useRecommendation(body, session) ? session.getAugmentedGraph() : session.getGraph();
        String partitionId = String.valueOf(body.get("partitionId"));
        EdgePartition<String> partition = session.getEdgePartitions().get(partitionId);
        if (partition == null) { ctx.status(400).json(Map.of("error", "Detect an edge partition first; it may have been invalidated by a graph edit.")); return; }
        String id = String.valueOf(body.get("metric"));
        boolean intra = individual && id.startsWith(EdgePartitionMetricCatalog.INTRA_PREFIX);
        String graphMetricId = intra ? id.substring(EdgePartitionMetricCatalog.INTRA_PREFIX.length()) : null;
        MetricCatalog.MetricDef def = intra ? MetricCatalog.graphMetrics().get(graphMetricId)
                : individual ? EdgePartitionMetricCatalog.individualMetrics().get(id)
                : EdgePartitionMetricCatalog.globalMetrics().get(id);
        if (def == null) { ctx.status(400).json(Map.of("error", "Unknown edge partition metric: " + id)); return; }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("metric", id);
        response.put("partitionId", partitionId);
        response.put("recommendation", MetricController.useRecommendation(body, session) ? session.getActiveRecommendationKey() : null);
        response.put("label", (intra ? "Intra-partition " : "") + def.label
                + Grids.suffix(def.params, MetricController.paramsOf(body)));
        try
        {
            if (individual)
            {
                IndividualEdgePartitionMetric<String> metric = EdgePartitionMetricCatalog.individual(id, MetricController.paramsOf(body));
                Map<Integer, Double> values = jobs.run(MetricController.jobKey(body, "edgePartitionIndividual"),
                        () -> metric.compute(metricGraph, partition));
                Map<String, Object> out = new LinkedHashMap<>();
                values.forEach((group, value) -> out.put(group.toString(), finite(value)));
                response.put("values", out);
                double average = values.values().stream().mapToDouble(Double::doubleValue)
                        .filter(Double::isFinite).average().orElse(Double.NaN);
                response.put("average", finite(average));
            }
            else
            {
                double value = jobs.run(MetricController.jobKey(body, "edgePartitionGlobal"),
                        () -> EdgePartitionMetricCatalog.<String>global(id).compute(metricGraph, partition));
                response.put("value", finite(value));
            }
            if (session.getEdgePartitions().get(partitionId) != partition)
            {
                ctx.status(409).json(Map.of("error", "The graph changed while computing edge partition metrics."));
                return;
            }
            ctx.json(response);
        }
        catch (JobCancelledException e) { ctx.json(Map.of("cancelled", true)); }
        catch (IllegalArgumentException e) { ctx.status(400).json(Map.of("error", e.getMessage())); }
        catch (Exception e) { ctx.status(500).json(Map.of("error", "Edge partition metric failed: " + e)); }
    }

    /** Undefined graph metrics (for example, on an empty group) are represented as null in JSON. */
    private static Double finite(double value)
    {
        return Double.isFinite(value) ? value : null;
    }

    private Communities<String> getCommunities(GraphSession session, Map<?, ?> body)
    {
        String partitionId = body.get("partition") == null ? null : String.valueOf(body.get("partition"));
        if (partitionId == null || partitionId.isEmpty()) return null;
        if (partitionId.startsWith("attribute:"))
        {
            String attribute = partitionId.substring("attribute:".length());
            if (session.getGraph().getNodeAttributeType(attribute) == null) return null;
            return new AttributeCommunityDetector<String>(attribute).detectCommunities(session.getGraph());
        }
        return session.getCommunities().get(partitionId);
    }

    private static String requiredString(Map<?, ?> body, String key)
    {
        Object value = body.get(key);
        if (value == null || value.toString().trim().isEmpty())
            throw new IllegalArgumentException("Choose " + key + " first.");
        return value.toString();
    }

    private static Double optionalDouble(Map<?, ?> body, String key)
    {
        Object value = body.get(key);
        if (value == null || value.toString().trim().isEmpty()) return null;
        try { return Double.valueOf(value.toString()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(key + " must be numeric."); }
    }

    private static String display(Object value)
    {
        return value == null ? "Unknown" : String.valueOf(value);
    }

    private static String communityName(int id)
    {
        return id < 0 ? "Unassigned" : "Community " + id;
    }
}
