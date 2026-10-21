package es.uam.eps.ir.relison.sna.metrics.communities.indiv;

import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;

/** Boundary-node count divided by the number of members. The empty-community value is zero. Direction and edge multiplicity follow BoundaryNodes. */
public class BoundaryNodeRatio<U> extends AbstractCommunityBoundaryMetric<U>
{
    public BoundaryNodeRatio() { this(EdgeOrientation.UND); }
    public BoundaryNodeRatio(EdgeOrientation orientation) { super(orientation, false); }

    @Override
    protected double evaluate(CommunityBoundaryStatistics<U> s, int group)
    {
        return s.members[group] == 0 ? 0.0 : (double) s.boundaryNodes(group, orientation) / s.members[group];
    }
}
