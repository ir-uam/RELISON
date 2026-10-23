package es.uam.eps.ir.relison.sna.metrics.edgepartition.graph;

import es.uam.eps.ir.relison.sna.metrics.edgepartition.AbstractEdgePartitionMetric;
import es.uam.eps.ir.relison.sna.metrics.edgepartition.EdgePartitionStatistics;

/** Fraction of graph edges assigned to a group; zero for an edgeless graph. */
public class AssignmentCoverage<U> extends AbstractEdgePartitionMetric<U>
{
    @Override
    protected EdgePartitionStatistics.Detail statisticsDetail()
    {
        return EdgePartitionStatistics.Detail.COUNTS;
    }

    @Override
    protected double evaluate(EdgePartitionStatistics<U> s)
    {
        return s.graphEdgeCount == 0 ? 0.0 : (double) s.assignedEdgeCount / s.graphEdgeCount;
    }
}
