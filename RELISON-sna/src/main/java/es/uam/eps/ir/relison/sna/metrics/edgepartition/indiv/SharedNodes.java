package es.uam.eps.ir.relison.sna.metrics.edgepartition.indiv;

import es.uam.eps.ir.relison.sna.metrics.edgepartition.AbstractIndividualEdgePartitionMetric;
import es.uam.eps.ir.relison.sna.metrics.edgepartition.EdgePartitionStatistics;

/** Number of group endpoints incident to edges assigned to another group. */
public class SharedNodes<U> extends AbstractIndividualEdgePartitionMetric<U>
{
    @Override
    protected double evaluate(EdgePartitionStatistics<U> s, int group)
    {
        return s.sharedNodes(group);
    }
}
