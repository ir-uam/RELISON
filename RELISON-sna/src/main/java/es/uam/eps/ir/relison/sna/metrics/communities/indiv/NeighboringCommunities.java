package es.uam.eps.ir.relison.sna.metrics.communities.indiv;

import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;

/** Number of distinct other community ids reached across the boundary, not the count of crossing edges. OUT/IN select outgoing/incoming neighbors; UND takes their union. Unassigned nodes are excluded from this count. */
public class NeighboringCommunities<U> extends AbstractCommunityBoundaryMetric<U>
{
    public NeighboringCommunities() { this(EdgeOrientation.UND); }
    public NeighboringCommunities(EdgeOrientation orientation) { super(orientation, false); }

    @Override
    protected double evaluate(CommunityBoundaryStatistics<U> s, int group)
    {
        return s.neighboringCommunities(group, orientation);
    }
}
