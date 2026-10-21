package es.uam.eps.ir.relison.sna.metrics.communities.indiv;

import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;

/** Number of members incident to at least one external edge. OUT counts outgoing, IN incoming, UND either; undirected orientation is ignored. Parallel edges do not duplicate nodes and loops are never external. */
public class BoundaryNodes<U> extends AbstractCommunityBoundaryMetric<U>
{
    public BoundaryNodes() { this(EdgeOrientation.UND); }
    public BoundaryNodes(EdgeOrientation orientation) { super(orientation, false); }

    @Override
    protected double evaluate(CommunityBoundaryStatistics<U> s, int group)
    {
        return s.boundaryNodes(group, orientation);
    }
}
