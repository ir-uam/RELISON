package es.uam.eps.ir.relison.sna.metrics.edgepartition.graph;

import es.uam.eps.ir.relison.sna.metrics.edgepartition.AbstractEdgePartitionMetric;
import es.uam.eps.ir.relison.sna.metrics.edgepartition.EdgePartitionStatistics;

/** Shannon entropy in bits over assigned-edge counts. Empty groups contribute zero. */
public class GroupSizeEntropy<U> extends AbstractEdgePartitionMetric<U>
{
    @Override
    protected EdgePartitionStatistics.Detail statisticsDetail()
    {
        return EdgePartitionStatistics.Detail.COUNTS;
    }

    @Override
    protected double evaluate(EdgePartitionStatistics<U> s)
    {
        if (s.assignedEdgeCount == 0) return 0.0;
        return java.util.Arrays.stream(s.groupSizes).filter(size -> size > 0).mapToDouble(size -> {
            double share = (double) size / s.assignedEdgeCount;
            return -share * Math.log(share) / Math.log(2.0);
        }).sum();
    }
}
