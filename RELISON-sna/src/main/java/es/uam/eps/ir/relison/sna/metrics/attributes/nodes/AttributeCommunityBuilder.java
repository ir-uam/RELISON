package es.uam.eps.ir.relison.sna.metrics.attributes.nodes;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.sna.community.Communities;

import java.util.LinkedHashMap;
import java.util.Map;

/** Builds a node-attribute partition, with every missing value assigned to one shared community. */
public class AttributeCommunityBuilder<U>
{
    public AttributePartition<U, Object> buildPartition(Graph<U> graph, String attribute)
    {
        if (graph.getNodeAttributeType(attribute) == null)
        {
            throw new IllegalArgumentException("Unknown node attribute: " + attribute);
        }

        Map<Integer, Object> communityToValue = new LinkedHashMap<>();
        Map<Object, Integer> valueToCommunity = new LinkedHashMap<>();
        Communities<U> communities = new Communities<>();

        graph.getAllNodes().forEach(user ->
        {
            Object value = graph.getNodeAttribute(user, attribute);
            Integer community = valueToCommunity.get(value);
            if (community == null && !valueToCommunity.containsKey(value))
            {
                community = valueToCommunity.size();
                valueToCommunity.put(value, community);
                communityToValue.put(community, value);
                communities.addCommunity();
            }
            communities.add(user, community);
        });

        return new AttributePartition<>(communities, communityToValue, valueToCommunity);
    }
}