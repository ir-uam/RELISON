package es.uam.eps.ir.relison.sna.metrics.edgepartition.graph;

import es.uam.eps.ir.relison.sna.metrics.edgepartition.AbstractEdgePartitionMetric;
import es.uam.eps.ir.relison.sna.metrics.edgepartition.EdgePartitionStatistics;

/** Fraction of nonisolated graph nodes incident to at least two assigned groups; zero if none exist. */
public class NodeMixingRatio<U> extends AbstractEdgePartitionMetric<U>
{
    @Override
    protected double evaluate(EdgePartitionStatistics<U> s)
    {
        return s.nodeGroups.isEmpty() ? 0.0 : (double) s.nodeGroups.values().stream().filter(groups -> groups.size() > 1).count() / s.nodeGroups.size();
    }
}
