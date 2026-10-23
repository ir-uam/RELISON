package es.uam.eps.ir.relison.sna.metrics.edgepartition.indiv;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.sna.edge.EdgePartition;

import es.uam.eps.ir.relison.sna.metrics.edgepartition.AbstractIndividualEdgePartitionMetric;
import es.uam.eps.ir.relison.sna.metrics.edgepartition.EdgePartitionStatistics;

/** Group edge count divided by the whole graph edge count; zero for an edgeless graph. */
public class EdgeShare<U> extends AbstractIndividualEdgePartitionMetric<U>
{
    @Override
    public double compute(Graph<U> graph,
                          EdgePartition<U> partition, int group)
    {
        if (group < 0 || group >= partition.getNumPartitions())
            throw new IllegalArgumentException("Invalid edge group: " + group);
        long total = graph.getEdgeCount();
        return total == 0 ? 0.0 : (double) partition.getPartitionSize(group) / total;
    }

    @Override
    protected EdgePartitionStatistics.Detail statisticsDetail()
    {
        return EdgePartitionStatistics.Detail.COUNTS;
    }

    @Override
    protected double evaluate(EdgePartitionStatistics<U> s, int group)
    {
        return s.graphEdgeCount == 0 ? 0.0 : (double) s.groupSizes[group] / s.graphEdgeCount;
    }
}
