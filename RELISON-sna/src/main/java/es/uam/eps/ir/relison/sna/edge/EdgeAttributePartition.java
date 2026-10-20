package es.uam.eps.ir.relison.sna.edge;

import java.util.Map;

/** Edge partition that also retains the edge-attribute value represented by each group. */
public class EdgeAttributePartition<U, A> extends EdgePartition<U>
{
    private final Map<Integer, A> valueLabels;
    private final Map<A, Integer> reverseLabels;

    public EdgeAttributePartition(Map<Integer, A> valueLabels, Map<A, Integer> reverseLabels)
    {
        this.valueLabels = valueLabels;
        this.reverseLabels = reverseLabels;
    }

    /** @return the attribute value represented by a group, or null if its id is invalid. */
    public A getAttributeValue(int partition)
    {
        return valueLabels.get(partition);
    }

    /** @return the group for an attribute value, or -1 if it is not represented. */
    public int getPartition(A value)
    {
        Integer partition = reverseLabels.get(value);
        return partition == null ? -1 : partition;
    }
}
