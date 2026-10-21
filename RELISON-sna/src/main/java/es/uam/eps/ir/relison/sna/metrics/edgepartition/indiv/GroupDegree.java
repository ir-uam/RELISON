package es.uam.eps.ir.relison.sna.metrics.edgepartition.indiv;

import es.uam.eps.ir.relison.sna.metrics.edgepartition.AbstractIndividualEdgePartitionMetric;
import es.uam.eps.ir.relison.sna.metrics.edgepartition.EdgePartitionStatistics;

/** Number of distinct other groups sharing an endpoint with this group. */
public class GroupDegree<U> extends AbstractIndividualEdgePartitionMetric<U>
{
    @Override
    protected double evaluate(EdgePartitionStatistics<U> s, int group)
    {
        return s.groupDegree(group);
    }
}
