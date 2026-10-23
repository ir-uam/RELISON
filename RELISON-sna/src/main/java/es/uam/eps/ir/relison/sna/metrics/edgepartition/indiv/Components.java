package es.uam.eps.ir.relison.sna.metrics.edgepartition.indiv;

import es.uam.eps.ir.relison.sna.metrics.edgepartition.AbstractIndividualEdgePartitionMetric;
import es.uam.eps.ir.relison.sna.metrics.edgepartition.EdgePartitionStatistics;

/** Number of weakly connected components using only the group's edges. */
public class Components<U> extends AbstractIndividualEdgePartitionMetric<U>
{
    @Override
    protected EdgePartitionStatistics.Detail statisticsDetail()
    {
        return EdgePartitionStatistics.Detail.NODES;
    }

    @Override
    protected double evaluate(EdgePartitionStatistics<U> s, int group)
    {
        return s.componentSizes(group).size();
    }
}
