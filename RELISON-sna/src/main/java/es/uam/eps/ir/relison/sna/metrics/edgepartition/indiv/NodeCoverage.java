package es.uam.eps.ir.relison.sna.metrics.edgepartition.indiv;

import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.sna.metrics.edgepartition.AbstractIndividualEdgePartitionMetric;
import es.uam.eps.ir.relison.sna.metrics.edgepartition.EdgePartitionStatistics;
import java.util.HashSet;
import java.util.Set;

/** Distinct incident nodes, or distinct sources/targets in a directed graph. */
public class NodeCoverage<U> extends AbstractIndividualEdgePartitionMetric<U>
{
    private final EdgeOrientation orientation;

    public NodeCoverage() { this(EdgeOrientation.UND); }

    public NodeCoverage(EdgeOrientation orientation)
    {
        if (orientation != EdgeOrientation.UND && orientation != EdgeOrientation.IN && orientation != EdgeOrientation.OUT)
            throw new IllegalArgumentException("Node coverage orientation must be UND, IN or OUT.");
        this.orientation = orientation;
    }

    @Override
    protected EdgePartitionStatistics.Detail statisticsDetail()
    {
        return EdgePartitionStatistics.Detail.NODES;
    }

    @Override
    protected double evaluate(EdgePartitionStatistics<U> s, int group)
    {
        if (!s.graph.isDirected() || orientation == EdgeOrientation.UND) return s.nodes.get(group).size();
        Set<U> nodes = new HashSet<>();
        s.edges.get(group).forEach(edge -> nodes.add(orientation == EdgeOrientation.OUT ? edge.getSource() : edge.getTarget()));
        return nodes.size();
    }
}
