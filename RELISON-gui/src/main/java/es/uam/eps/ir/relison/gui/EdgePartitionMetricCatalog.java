package es.uam.eps.ir.relison.gui;

import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.sna.metrics.EdgePartitionMetric;
import es.uam.eps.ir.relison.sna.metrics.IndividualEdgePartitionMetric;
import es.uam.eps.ir.relison.sna.metrics.edgepartition.indiv.*;
import es.uam.eps.ir.relison.sna.metrics.edgepartition.graph.*;
import java.util.*;

/** GUI catalog for global, per-group and restricted-subgraph edge partition metrics. */
public final class EdgePartitionMetricCatalog
{
    public static final String INTRA_PREFIX = "intra-edge-partition:";
    private static final Map<String, MetricCatalog.MetricDef> INDIVIDUAL = new LinkedHashMap<>();
    private static final Map<String, MetricCatalog.MetricDef> GLOBAL = new LinkedHashMap<>();
    static
    {
        INDIVIDUAL.put("size", new MetricCatalog.MetricDef("size", "Size", List.of()));
        INDIVIDUAL.put("edge-share", new MetricCatalog.MetricDef("edge-share", "Edge share", List.of()));
        INDIVIDUAL.put("node-coverage", new MetricCatalog.MetricDef("node-coverage", "Node coverage", List.of()));
        INDIVIDUAL.put("source-coverage", new MetricCatalog.MetricDef("source-coverage", "Source node coverage", List.of()));
        INDIVIDUAL.put("target-coverage", new MetricCatalog.MetricDef("target-coverage", "Target node coverage", List.of()));
        INDIVIDUAL.put("total-weight", new MetricCatalog.MetricDef("total-weight", "Total weight", List.of()));
        INDIVIDUAL.put("shared-nodes", new MetricCatalog.MetricDef("shared-nodes", "Shared nodes", List.of()));
        INDIVIDUAL.put("shared-node-ratio", new MetricCatalog.MetricDef("shared-node-ratio", "Shared-node ratio", List.of()));
        INDIVIDUAL.put("group-degree", new MetricCatalog.MetricDef("group-degree", "Group degree", List.of()));
        INDIVIDUAL.put("components", new MetricCatalog.MetricDef("components", "Weakly connected components", List.of()));
        INDIVIDUAL.put("largest-component-share", new MetricCatalog.MetricDef("largest-component-share", "Largest weak component share", List.of()));
        GLOBAL.put("num-groups", new MetricCatalog.MetricDef("num-groups", "Number of groups", List.of()));
        GLOBAL.put("nonempty-groups", new MetricCatalog.MetricDef("nonempty-groups", "Number of nonempty groups", List.of()));
        GLOBAL.put("assignment-coverage", new MetricCatalog.MetricDef("assignment-coverage", "Assignment coverage", List.of()));
        GLOBAL.put("group-size-gini", new MetricCatalog.MetricDef("group-size-gini", "Group size Gini", List.of()));
        GLOBAL.put("group-weight-gini", new MetricCatalog.MetricDef("group-weight-gini", "Group weight Gini", List.of()));
        GLOBAL.put("group-size-entropy", new MetricCatalog.MetricDef("group-size-entropy", "Group size entropy (bits)", List.of()));
        GLOBAL.put("node-mixing-ratio", new MetricCatalog.MetricDef("node-mixing-ratio", "Node mixing ratio", List.of()));
        GLOBAL.put("mean-node-participation", new MetricCatalog.MetricDef("mean-node-participation", "Mean node participation", List.of()));
    }
    private EdgePartitionMetricCatalog() { }
    public static Map<String, MetricCatalog.MetricDef> individualMetrics() { return INDIVIDUAL; }
    public static Map<String, MetricCatalog.MetricDef> globalMetrics() { return GLOBAL; }

    public static List<Map<String, Object>> intraJson()
    {
        List<Map<String, Object>> result = new ArrayList<>();
        MetricCatalog.graphMetrics().values().forEach(def -> result.add(Map.of(
                "id", INTRA_PREFIX + def.id, "label", "Intra-partition " + def.label,
                "params", MetricCatalog.paramsJson(def.params))));
        return result;
    }

    public static <U> IndividualEdgePartitionMetric<U> individual(String id)
    {
        switch (id)
        {
            case "size": return new Size<>();
            case "edge-share": return new EdgeShare<>();
            case "node-coverage": return new NodeCoverage<>();
            case "source-coverage": return new NodeCoverage<>(EdgeOrientation.OUT);
            case "target-coverage": return new NodeCoverage<>(EdgeOrientation.IN);
            case "total-weight": return new TotalWeight<>();
            case "shared-nodes": return new SharedNodes<>();
            case "shared-node-ratio": return new SharedNodeRatio<>();
            case "group-degree": return new GroupDegree<>();
            case "components": return new Components<>();
            case "largest-component-share": return new LargestComponentShare<>();
            default: throw new IllegalArgumentException("Unknown individual edge partition metric: " + id);
        }
    }

    public static <U> EdgePartitionMetric<U> global(String id)
    {
        switch (id)
        {
            case "num-groups": return new NumberOfGroups<>();
            case "nonempty-groups": return new NumberOfGroups<>(true);
            case "assignment-coverage": return new AssignmentCoverage<>();
            case "group-size-gini": return new GroupSizeGini<>();
            case "group-weight-gini": return new GroupWeightGini<>();
            case "group-size-entropy": return new GroupSizeEntropy<>();
            case "node-mixing-ratio": return new NodeMixingRatio<>();
            case "mean-node-participation": return new MeanNodeParticipation<>();
            default: throw new IllegalArgumentException("Unknown global edge partition metric: " + id);
        }
    }
}
