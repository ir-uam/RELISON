package es.uam.eps.ir.relison.sna.metrics.edgepartition.indiv;

import es.uam.eps.ir.relison.sna.metrics.edgepartition.AbstractIndividualEdgePartitionMetric;
import es.uam.eps.ir.relison.sna.metrics.edgepartition.EdgePartitionStatistics;

/** Fraction of covered nodes shared with other groups; zero for an empty group. */
public class SharedNodeRatio<U> extends AbstractIndividualEdgePartitionMetric<U>
{
    @Override
    protected double evaluate(EdgePartitionStatistics<U> s, int group)
    {
        return s.nodes.get(group).isEmpty() ? 0.0 : (double) s.sharedNodes(group) / s.nodes.get(group).size();
    }
}
