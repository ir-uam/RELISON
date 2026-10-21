package es.uam.eps.ir.relison.sna.edge.detection.attributes;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.attributes.AttributeType;
import es.uam.eps.ir.relison.graph.multigraph.MultiGraph;
import es.uam.eps.ir.relison.sna.edge.Edge;
import es.uam.eps.ir.relison.sna.edge.EdgeAttributePartition;
import es.uam.eps.ir.relison.sna.edge.detection.EdgePartitionDetector;
import es.uam.eps.ir.relison.sna.edge.detection.EdgePartitionSupport;
import es.uam.eps.ir.relison.sna.community.detection.attributes.NumericAttributeBin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/** Partitions edges by one declared edge attribute, optionally binning numeric values. */
public class EdgeAttributePartitionDetector<U> implements EdgePartitionDetector<U>
{
    private final String attribute;
    private final Double binWidth;
    private final BigDecimal decimalWidth;

    public EdgeAttributePartitionDetector(String attribute)
    {
        this(attribute, null);
    }

    /** Numeric bins are anchored at zero and include their lower, but not upper, bound. */
    public EdgeAttributePartitionDetector(String attribute, Double binWidth)
    {
        if (attribute == null || attribute.trim().isEmpty())
            throw new IllegalArgumentException("Attribute name cannot be empty");
        this.attribute = attribute;
        if (binWidth != null && (!Double.isFinite(binWidth) || binWidth <= 0.0))
            throw new IllegalArgumentException("Numeric bin width must be finite and positive");
        this.binWidth = binWidth;
        this.decimalWidth = binWidth == null ? null : BigDecimal.valueOf(binWidth);
    }

    public String getAttribute()
    {
        return attribute;
    }

    @Override
    public EdgeAttributePartition<U, Object> detectEdgePartition(Graph<U> graph)
    {
        if (graph == null) return null;
        AttributeType type = graph.getEdgeAttributeType(attribute);
        if (type == null)
            throw new IllegalArgumentException("Unknown edge attribute: " + attribute);
        if (binWidth != null && !type.isNumeric())
            throw new IllegalArgumentException("Numeric binning requires a numeric edge attribute: " + attribute);

        Map<Integer, Object> labels = new LinkedHashMap<>();
        Map<Object, Integer> reverseLabels = new LinkedHashMap<>();
        EdgeAttributePartition<U, Object> partition = new EdgeAttributePartition<>(labels, reverseLabels);
        EdgePartitionSupport.forEachEdge(graph, edge ->
        {
            Object value = bin(getValue(graph, edge));
            Integer group = reverseLabels.get(value);
            if (group == null && !reverseLabels.containsKey(value))
            {
                group = labels.size();
                labels.put(group, value);
                reverseLabels.put(value, group);
                partition.addPartition();
            }
            partition.add(edge, group);
        });
        return partition;
    }

    private Object bin(Object value)
    {
        if (value == null || binWidth == null) return value;
        Number number = (Number) value;
        if (!Double.isFinite(number.doubleValue()))
            throw new IllegalArgumentException("Cannot bin a non-finite value for edge attribute: " + attribute);
        BigDecimal lower = new BigDecimal(number.toString()).divide(decimalWidth, 0, RoundingMode.FLOOR).multiply(decimalWidth);
        return new NumericAttributeBin(lower, lower.add(decimalWidth));
    }

    private Object getValue(Graph<U> graph, Edge<U> edge)
    {
        if (graph instanceof MultiGraph<?>)
        {
            MultiGraph<U> multiGraph = (MultiGraph<U>) graph;
            return multiGraph.getEdgeAttribute(edge.getSource(), edge.getTarget(), edge.getIndex(), attribute);
        }
        return graph.getEdgeAttribute(edge.getSource(), edge.getTarget(), attribute);
    }
}
