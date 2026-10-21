package es.uam.eps.ir.relison.sna.metrics.edgepartition.graph;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.sna.edge.EdgePartition;

import es.uam.eps.ir.relison.sna.metrics.edgepartition.AbstractEdgePartitionMetric;
import es.uam.eps.ir.relison.sna.metrics.edgepartition.EdgePartitionStatistics;

/** Counts all declared groups, or only nonempty groups when requested. */
public class NumberOfGroups<U> extends AbstractEdgePartitionMetric<U>
{
    private final boolean nonemptyOnly;
    public NumberOfGroups() { this(false); }
    public NumberOfGroups(boolean nonemptyOnly) { this.nonemptyOnly = nonemptyOnly; }

    @Override
    public double compute(Graph<U> graph,
                          EdgePartition<U> partition)
    {
        return nonemptyOnly ? partition.getPartitions().filter(group -> partition.getPartitionSize(group) > 0).count()
                : partition.getNumPartitions();
    }

    @Override
    protected EdgePartitionStatistics.Detail statisticsDetail()
    {
        return EdgePartitionStatistics.Detail.COUNTS;
    }

    @Override
    protected double evaluate(EdgePartitionStatistics<U> s)
    {
        return nonemptyOnly ? java.util.Arrays.stream(s.groupSizes).filter(size -> size > 0).count() : s.groupSizes.length;
    }
}
