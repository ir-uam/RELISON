package es.uam.eps.ir.relison.sna.community.detection.attributes;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.sna.community.detection.CommunityDetectionAlgorithm;

/** Creates one community for each distinct value of a node attribute. Missing values form one community. */
public class AttributeCommunityDetector<U> implements CommunityDetectionAlgorithm<U>
{
    private final String attribute;
    private final AttributeCommunityBuilder<U> builder;

    public AttributeCommunityDetector(String attribute)
    {
        if (attribute == null || attribute.trim().isEmpty())
        {
            throw new IllegalArgumentException("Attribute name cannot be empty");
        }
        this.attribute = attribute;
        this.builder = new AttributeCommunityBuilder<>();
    }

    /** @return the node attribute used to form communities. */
    public String getAttribute()
    {
        return attribute;
    }

    @Override
    public Communities<U> detectCommunities(Graph<U> graph)
    {
        return detectAttributePartition(graph).getCommunities();
    }

    /**
     * Detects communities and retains the mapping between community ids and attribute values.
     * @param graph graph whose nodes are partitioned.
     * @return the detected partition with its value labels.
     */
    public AttributePartition<U, Object> detectAttributePartition(Graph<U> graph)
    {
        if (graph == null) return null;
        return builder.buildPartition(graph, attribute);
    }
}
