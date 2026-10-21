package es.uam.eps.ir.relison.sna.metrics.edgepartition.graph;

import es.uam.eps.ir.relison.sna.metrics.edgepartition.AbstractEdgePartitionMetric;
import es.uam.eps.ir.relison.sna.metrics.edgepartition.EdgePartitionStatistics;

/** Mean number of assigned groups per nonisolated graph node, including zero memberships. */
public class MeanNodeParticipation<U> extends AbstractEdgePartitionMetric<U>
{
    @Override
    protected double evaluate(EdgePartitionStatistics<U> s)
    {
        return s.nodeGroups.values().stream().mapToInt(java.util.Set::size).average().orElse(0.0);
    }
}
