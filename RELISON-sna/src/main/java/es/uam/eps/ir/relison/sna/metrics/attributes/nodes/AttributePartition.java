package es.uam.eps.ir.relison.sna.metrics.attributes.nodes;

import es.uam.eps.ir.relison.sna.community.Communities;

import java.util.Map;

public class AttributePartition<U, A>
{
    private final Communities<U> partition;
    private final Map<Integer, A> valueLabels;
    private final Map<A, Integer> reverseLabels;

    public AttributePartition(Communities<U> partition, Map<Integer, A> valueLabels, Map<A, Integer> reverseLabels)
    {
        this.partition = partition;
        this.valueLabels = valueLabels;
        this.reverseLabels = reverseLabels;
    }

    public Communities<U> getCommunities()
    {
        return partition;
    }

    public Map<Integer, A> getMapping()
    {
        return valueLabels;
    }

    public int getCommNumber(A value)
    {
        return this.reverseLabels.get(value);
    }

    public A getAttributeValue(int index)
    {
        return this.valueLabels.get(index);
    }
}
