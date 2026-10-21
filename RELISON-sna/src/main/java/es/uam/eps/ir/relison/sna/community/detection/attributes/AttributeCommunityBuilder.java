package es.uam.eps.ir.relison.sna.community.detection.attributes;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.utils.ComputationMonitor;
import es.uam.eps.ir.relison.graph.attributes.AttributeType;
import es.uam.eps.ir.relison.sna.community.Communities;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/** Builds a node-attribute partition, with every missing value assigned to one shared community. */
public class AttributeCommunityBuilder<U>
{
    public AttributePartition<U, Object> buildPartition(Graph<U> graph, String attribute)
    {
        return buildPartition(graph, attribute, null);
    }

    /** Builds communities using fixed-width numeric bins when a bin width is provided. */
    public AttributePartition<U, Object> buildPartition(Graph<U> graph, String attribute, Double binWidth)
    {
        AttributeType type = graph.getNodeAttributeType(attribute);
        if (type == null)
        {
            throw new IllegalArgumentException("Unknown node attribute: " + attribute);
        }
        if (binWidth != null && (!Double.isFinite(binWidth) || binWidth <= 0.0))
            throw new IllegalArgumentException("Numeric bin width must be finite and positive");
        if (binWidth != null && !type.isNumeric())
            throw new IllegalArgumentException("Numeric binning requires a numeric node attribute: " + attribute);

        Map<Integer, Object> communityToValue = new LinkedHashMap<>();
        Map<Object, Integer> valueToCommunity = new LinkedHashMap<>();
        Communities<U> communities = new Communities<>();

        ComputationMonitor monitor = ComputationMonitor.current();
        long[] processed = {0};
        graph.getAllNodes().forEach(user ->
        {
            monitor.checkCancelled();
            Object value = graph.getNodeAttribute(user, attribute);
            if (value != null && binWidth != null)
            {
                Number number = (Number) value;
                if (number instanceof Double && !Double.isFinite(number.doubleValue()))
                    throw new IllegalArgumentException("Cannot bin a non-finite value for node attribute: " + attribute);
                BigDecimal width = BigDecimal.valueOf(binWidth);
                BigDecimal numeric = new BigDecimal(number.toString());
                BigDecimal lower = numeric.divide(width, 0, RoundingMode.FLOOR).multiply(width);
                value = new NumericAttributeBin(lower, lower.add(width));
            }
            Integer community = valueToCommunity.get(value);
            if (community == null && !valueToCommunity.containsKey(value))
            {
                community = valueToCommunity.size();
                valueToCommunity.put(value, community);
                communityToValue.put(community, value);
                communities.addCommunity();
            }
            communities.add(user, community);
            long count = ++processed[0];
            if (count == 1 || (count & 255) == 0 || count == graph.getVertexCount())
                monitor.progress("Grouping nodes by attribute", count, graph.getVertexCount(), "nodes");
        });

        return new AttributePartition<>(communities, communityToValue, valueToCommunity);
    }
}
