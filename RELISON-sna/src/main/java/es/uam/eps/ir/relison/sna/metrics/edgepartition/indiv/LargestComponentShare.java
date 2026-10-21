package es.uam.eps.ir.relison.sna.metrics.edgepartition.indiv;

import es.uam.eps.ir.relison.sna.metrics.edgepartition.AbstractIndividualEdgePartitionMetric;
import es.uam.eps.ir.relison.sna.metrics.edgepartition.EdgePartitionStatistics;

/** Covered-node fraction in the largest weak component; zero for an empty group. */
public class LargestComponentShare<U> extends AbstractIndividualEdgePartitionMetric<U>
{
    @Override
    protected EdgePartitionStatistics.Detail statisticsDetail()
    {
        return EdgePartitionStatistics.Detail.NODES;
    }

    @Override
    protected double evaluate(EdgePartitionStatistics<U> s, int group)
    {
        return s.nodes.get(group).isEmpty() ? 0.0 : (double) s.componentSizes(group).stream().mapToInt(Integer::intValue).max().orElse(0) / s.nodes.get(group).size();
    }
}
