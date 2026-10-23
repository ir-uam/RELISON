package es.uam.eps.ir.relison.sna.edge.detection.attributes;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.attributes.AttributeType;
import es.uam.eps.ir.relison.sna.edge.Edge;
import es.uam.eps.ir.relison.sna.edge.NodeAttributeRelationPartition;
import es.uam.eps.ir.relison.sna.edge.detection.EdgePartitionDetector;
import es.uam.eps.ir.relison.sna.edge.detection.EdgePartitionSupport;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Groups edges by the exact pair of endpoint node-attribute values, optionally binning numeric values. */
public class NodeAttributeRelationEdgePartitionDetector<U> implements EdgePartitionDetector<U>
{
    private final String attribute;
    private final Double binWidth;

    /** Creates exact-value groups. */
    public NodeAttributeRelationEdgePartitionDetector(String attribute)
    {
        this(attribute, null);
    }

    /**
     * Creates groups by endpoint value pairs, optionally mapping numeric values to fixed-width bins.
     * Bin intervals are lower-inclusive and upper-exclusive, anchored at zero.
     * @param attribute node attribute to use.
     * @param binWidth positive bin width, or null to use exact values.
     */
    public NodeAttributeRelationEdgePartitionDetector(String attribute, Double binWidth)
    {
        if (attribute == null || attribute.trim().isEmpty())
            throw new IllegalArgumentException("Attribute name cannot be empty");
        if (binWidth != null && (!Double.isFinite(binWidth) || binWidth <= 0.0))
            throw new IllegalArgumentException("Numeric bin width must be finite and positive");
        this.attribute = attribute;
        this.binWidth = binWidth;
    }

    public String getAttribute()
    {
        return attribute;
    }

    /** @return the configured numeric bin width, or null when exact values are used. */
    public Double getBinWidth()
    {
        return binWidth;
    }

    @Override
    public NodeAttributeRelationPartition<U> detectEdgePartition(Graph<U> graph)
    {
        if (graph == null) return null;
        AttributeType type = graph.getNodeAttributeType(attribute);
        if (type == null) throw new IllegalArgumentException("Unknown node attribute: " + attribute);
        if (binWidth != null && !type.isNumeric())
            throw new IllegalArgumentException("Numeric binning requires a numeric node attribute: " + attribute);

        Map<Integer, String> labels = new LinkedHashMap<>();
        Map<ValuePair, Integer> groups = new LinkedHashMap<>();
        NodeAttributeRelationPartition<U> partition = new NodeAttributeRelationPartition<>(labels);
        EdgePartitionSupport.forEachEdge(graph, edge ->
        {
            Object sourceValue = value(graph, edge.getSource());
            Object targetValue = value(graph, edge.getTarget());
            ValuePair pair = new ValuePair(sourceValue, targetValue, graph.isDirected());
            Integer group = groups.get(pair);
            if (group == null)
            {
                group = groups.size();
                groups.put(pair, group);
                labels.put(group, pair.toDisplayLabel(graph.isDirected()));
                partition.addPartition();
            }
            partition.add(edge, group);
        });
        return partition;
    }

    private Object value(Graph<U> graph, U node)
    {
        Object value = graph.getNodeAttribute(node, attribute);
        if (value == null || binWidth == null) return value;
        Number number = (Number) value;
        if (number instanceof Double && !Double.isFinite(number.doubleValue()))
            throw new IllegalArgumentException("Cannot bin a non-finite value for node attribute: " + attribute);
        BigDecimal width = BigDecimal.valueOf(binWidth);
        BigDecimal numeric = new BigDecimal(number.toString());
        BigDecimal bin = numeric.divide(width, 0, RoundingMode.FLOOR);
        BigDecimal lower = bin.multiply(width).stripTrailingZeros();
        return new NumericBin(lower, lower.add(width).stripTrailingZeros());
    }

    private static final class ValuePair
    {
        private final Object source;
        private final Object target;

        private ValuePair(Object source, Object target, boolean directed)
        {
            if (directed || compare(source, target) <= 0)
            {
                this.source = source;
                this.target = target;
            }
            else
            {
                this.source = target;
                this.target = source;
            }
        }

        private static int compare(Object first, Object second)
        {
            String firstLabel = display(first);
            String secondLabel = display(second);
            int labelOrder = firstLabel.compareTo(secondLabel);
            if (labelOrder != 0) return labelOrder;
            String firstType = first == null ? "" : first.getClass().getName();
            String secondType = second == null ? "" : second.getClass().getName();
            return firstType.compareTo(secondType);
        }

        private String toDisplayLabel(boolean directed)
        {
            return display(source) + (directed ? " → " : " ↔ ") + display(target);
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other) return true;
            if (!(other instanceof ValuePair)) return false;
            ValuePair pair = (ValuePair) other;
            return Objects.equals(source, pair.source) && Objects.equals(target, pair.target);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(source, target);
        }
    }

    private static String display(Object value)
    {
        return value == null ? "Unknown" : String.valueOf(value);
    }

    private static final class NumericBin
    {
        private final BigDecimal lower;
        private final BigDecimal upper;

        private NumericBin(BigDecimal lower, BigDecimal upper)
        {
            this.lower = lower;
            this.upper = upper;
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other) return true;
            if (!(other instanceof NumericBin)) return false;
            NumericBin bin = (NumericBin) other;
            return lower.compareTo(bin.lower) == 0 && upper.compareTo(bin.upper) == 0;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(lower.stripTrailingZeros(), upper.stripTrailingZeros());
        }

        @Override
        public String toString()
        {
            return "[" + lower.toPlainString() + ", " + upper.toPlainString() + ")";
        }
    }
}
