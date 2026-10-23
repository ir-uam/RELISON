package es.uam.eps.ir.relison.sna.metrics.edgepartition.graph;

import es.uam.eps.ir.relison.sna.metrics.edgepartition.AbstractEdgePartitionMetric;
import es.uam.eps.ir.relison.sna.metrics.edgepartition.EdgePartitionStatistics;
import es.uam.eps.ir.relison.utils.indexes.GiniIndex;
import java.util.ArrayList;
import java.util.List;

/** Normalized Gini inequality (0 equal, 1 maximal), including empty groups; zero for zero total mass.
 */
public class GroupSizeGini<U> extends AbstractEdgePartitionMetric<U>
{
    @Override
    protected EdgePartitionStatistics.Detail statisticsDetail()
    {
        return EdgePartitionStatistics.Detail.COUNTS;
    }

    @Override
    protected double evaluate(EdgePartitionStatistics<U> s)
    {
        List<Double> values = new ArrayList<>();
        for (int size : s.groupSizes) values.add((double) size);
        if (values.size() <= 1) return 0.0;
        double maximum = values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        if (maximum == 0.0) return 0.0;
        // Scaling preserves Gini and avoids overflow when summing large group weights.
        values.replaceAll(value -> value / maximum);
        return new GiniIndex().compute(values, true);
    }
}
