package es.uam.eps.ir.relison.sna.edge;

import java.util.Map;

/** Edge partition retaining a display label for each pair of endpoint attribute values. */
public class NodeAttributeRelationPartition<U> extends EdgePartition<U>
{
    private final Map<Integer, String> relationLabels;

    public NodeAttributeRelationPartition(Map<Integer, String> relationLabels)
    {
        this.relationLabels = relationLabels;
    }

    /** @return the label for a relation group, or null if the group id is invalid. */
    public String getRelationLabel(int partition)
    {
        return relationLabels.get(partition);
    }
}
