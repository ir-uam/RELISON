package es.uam.eps.ir.relison.sna.metrics.edgepartition;

import es.uam.eps.ir.relison.utils.ComputationMonitor;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.sna.edge.EdgePartition;
import es.uam.eps.ir.relison.sna.metrics.IndividualEdgePartitionMetric;
import java.util.LinkedHashMap;
import java.util.Map;

/** Shares membership traversal across all groups in a single evaluation. */
public abstract class AbstractIndividualEdgePartitionMetric<U> implements IndividualEdgePartitionMetric<U>
{
    protected abstract double evaluate(EdgePartitionStatistics<U> statistics, int group);

    protected EdgePartitionStatistics.Detail statisticsDetail()
    {
        return EdgePartitionStatistics.Detail.MEMBERSHIPS;
    }

    @Override
    public double compute(Graph<U> graph, EdgePartition<U> partition, int group)
    {
        if (group < 0 || group >= partition.getNumPartitions())
            throw new IllegalArgumentException("Invalid edge group: " + group);
        return evaluate(new EdgePartitionStatistics<>(graph, partition, statisticsDetail()), group);
    }

    @Override
    public Map<Integer, Double> compute(Graph<U> graph, EdgePartition<U> partition)
    {
        EdgePartitionStatistics<U> statistics = new EdgePartitionStatistics<>(graph, partition, statisticsDetail());
        Map<Integer, Double> values = ComputationMonitor.current().track(new LinkedHashMap<>(), "Computing edge partition values", partition.getNumPartitions(), "groups");
        partition.getPartitions().forEach(group -> {
            if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
            values.put(group, evaluate(statistics, group));
        });
        return values;
    }

    @Override
    public double averageValue(Graph<U> graph, EdgePartition<U> partition)
    {
        return compute(graph, partition).values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
}
