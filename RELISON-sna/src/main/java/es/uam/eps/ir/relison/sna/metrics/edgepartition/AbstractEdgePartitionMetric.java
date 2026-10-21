package es.uam.eps.ir.relison.sna.metrics.edgepartition;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.sna.edge.EdgePartition;
import es.uam.eps.ir.relison.sna.metrics.EdgePartitionMetric;

public abstract class AbstractEdgePartitionMetric<U> implements EdgePartitionMetric<U>
{
    protected abstract double evaluate(EdgePartitionStatistics<U> statistics);

    protected EdgePartitionStatistics.Detail statisticsDetail()
    {
        return EdgePartitionStatistics.Detail.MEMBERSHIPS;
    }

    @Override
    public double compute(Graph<U> graph, EdgePartition<U> partition)
    {
        return evaluate(new EdgePartitionStatistics<>(graph, partition, statisticsDetail()));
    }
}
