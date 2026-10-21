package es.uam.eps.ir.relison.sna.metrics.edgepartition.indiv;

import es.uam.eps.ir.relison.sna.metrics.edgepartition.AbstractIndividualEdgePartitionMetric;
import es.uam.eps.ir.relison.sna.metrics.edgepartition.EdgePartitionStatistics;

/** Signed sum of finite edge weights; an unweighted edge contributes one. */
public class TotalWeight<U> extends AbstractIndividualEdgePartitionMetric<U>
{
    @Override
    protected EdgePartitionStatistics.Detail statisticsDetail()
    {
        return EdgePartitionStatistics.Detail.EDGES;
    }

    @Override
    protected double evaluate(EdgePartitionStatistics<U> s, int group)
    {
        return s.totalWeight(group);
    }
}
