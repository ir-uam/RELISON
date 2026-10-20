package es.uam.eps.ir.relison.sna.community.detection.attributes;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.sna.community.detection.CommunityDetectionAlgorithm;

/** Detects communities by assigning nodes with numeric attribute values to fixed-width bins. */
public class NumericAttributeCommunityDetector<U> implements CommunityDetectionAlgorithm<U>
{
    private final String attribute;
    private final double binWidth;
    private final AttributeCommunityBuilder<U> builder;

    /**
     * @param attribute numeric node attribute to partition.
     * @param binWidth positive bin width; bins are lower-inclusive, upper-exclusive, and anchored at zero.
     */
    public NumericAttributeCommunityDetector(String attribute, double binWidth)
    {
        if (attribute == null || attribute.trim().isEmpty())
            throw new IllegalArgumentException("Attribute name cannot be empty");
        if (!Double.isFinite(binWidth) || binWidth <= 0.0)
            throw new IllegalArgumentException("Numeric bin width must be finite and positive");
        this.attribute = attribute;
        this.binWidth = binWidth;
        this.builder = new AttributeCommunityBuilder<>();
    }

    public String getAttribute()
    {
        return attribute;
    }

    public double getBinWidth()
    {
        return binWidth;
    }

    @Override
    public Communities<U> detectCommunities(Graph<U> graph)
    {
        AttributePartition<U, Object> partition = detectAttributePartition(graph);
        return partition == null ? null : partition.getCommunities();
    }

    /** Detects communities and keeps the bin represented by each community id. */
    public AttributePartition<U, Object> detectAttributePartition(Graph<U> graph)
    {
        if (graph == null) return null;
        return builder.buildPartition(graph, attribute, binWidth);
    }
}
