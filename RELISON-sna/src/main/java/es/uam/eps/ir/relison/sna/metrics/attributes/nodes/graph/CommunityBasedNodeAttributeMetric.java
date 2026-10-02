package es.uam.eps.ir.relison.sna.metrics.attributes.nodes.graph;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.sna.metrics.CommunityMetric;
import es.uam.eps.ir.relison.sna.metrics.IndividualCommunityMetric;
import es.uam.eps.ir.relison.sna.metrics.IndividualNodeAttributeMetric;
import es.uam.eps.ir.relison.sna.metrics.NodeAttributeMetric;
import es.uam.eps.ir.relison.sna.metrics.attributes.nodes.AttributeCommunityBuilder;
import es.uam.eps.ir.relison.sna.metrics.attributes.nodes.AttributePartition;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.util.Map;

public class CommunityBasedNodeAttributeMetric<U> implements NodeAttributeMetric<U>
{
    private final CommunityMetric<U> metric;

    public CommunityBasedNodeAttributeMetric(CommunityMetric<U> metric)
    {
        this.metric = metric;
    }

    @Override
    public double compute(Graph<U> graph, String attr)
    {
        AttributeCommunityBuilder<U> builder = new AttributeCommunityBuilder<>();
        AttributePartition<U, Object> attrPartition = builder.buildPartition(graph, attr);
        return this.metric.compute(graph, attrPartition.getCommunities());
    }
}
