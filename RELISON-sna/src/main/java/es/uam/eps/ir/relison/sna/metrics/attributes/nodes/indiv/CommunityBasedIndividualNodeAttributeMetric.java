package es.uam.eps.ir.relison.sna.metrics.attributes.nodes.indiv;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.sna.metrics.IndividualCommunityMetric;
import es.uam.eps.ir.relison.sna.metrics.IndividualNodeAttributeMetric;
import es.uam.eps.ir.relison.sna.metrics.attributes.nodes.AttributeCommunityBuilder;
import es.uam.eps.ir.relison.sna.metrics.attributes.nodes.AttributePartition;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.util.Map;

public class CommunityBasedIndividualNodeAttributeMetric<U> implements IndividualNodeAttributeMetric<U>
{
    private final IndividualCommunityMetric<U> metric;

    public CommunityBasedIndividualNodeAttributeMetric(IndividualCommunityMetric<U> metric)
    {
        this.metric = metric;
    }

    @Override
    public double compute(Graph<U> graph, String attr, Object indiv)
    {
        AttributeCommunityBuilder<U> builder = new AttributeCommunityBuilder<>();
        AttributePartition<U, Object> attrPartition = builder.buildPartition(graph, attr);
        return this.metric.compute(graph, attrPartition.getCommunities(), attrPartition.getCommNumber(indiv));
    }

    @Override
    public Map<Object, Double> compute(Graph<U> graph, String attr) {
        AttributeCommunityBuilder<U> builder = new AttributeCommunityBuilder<>();
        AttributePartition<U, Object> attrPartition = builder.buildPartition(graph, attr);
        Map<Integer, Double> metric =this.metric.compute(graph, attrPartition.getCommunities());
        Map<Object, Double> def = new Object2DoubleOpenHashMap<>();
        metric.forEach((k, v) -> def.put(attrPartition.getAttributeValue(k), v));
        return def;
    }

    @Override
    public double averageValue(Graph<U> graph, String attr)
    {
        AttributeCommunityBuilder<U> builder = new AttributeCommunityBuilder<>();
        AttributePartition<U, Object> attrPartition = builder.buildPartition(graph, attr);
        return this.metric.averageValue(graph, attrPartition.getCommunities());
    }
}
