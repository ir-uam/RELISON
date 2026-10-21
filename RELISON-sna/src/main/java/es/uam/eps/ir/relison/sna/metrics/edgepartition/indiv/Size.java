package es.uam.eps.ir.relison.sna.metrics.edgepartition.indiv;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.sna.edge.EdgePartition;

import es.uam.eps.ir.relison.sna.metrics.edgepartition.AbstractIndividualEdgePartitionMetric;
import es.uam.eps.ir.relison.sna.metrics.edgepartition.EdgePartitionStatistics;

/** Number of edges in the group; parallel edges and loops count individually. */
public class Size<U> extends AbstractIndividualEdgePartitionMetric<U>
{
    @Override
    public double compute(Graph<U> graph,
                          EdgePartition<U> partition, int group)
    {
        if (group < 0 || group >= partition.getNumPartitions())
            throw new IllegalArgumentException("Invalid edge group: " + group);
        return partition.getPartitionSize(group);
    }

    @Override
    protected EdgePartitionStatistics.Detail statisticsDetail()
    {
        return EdgePartitionStatistics.Detail.COUNTS;
    }

    @Override
    protected double evaluate(EdgePartitionStatistics<U> s, int group)
    {
        return s.groupSizes[group];
    }
}
