package es.uam.eps.ir.relison.sna.metrics.edgepartition.graph;

import es.uam.eps.ir.relison.sna.metrics.edgepartition.AbstractEdgePartitionMetric;
import es.uam.eps.ir.relison.sna.metrics.edgepartition.EdgePartitionStatistics;
import es.uam.eps.ir.relison.utils.indexes.GiniIndex;
import java.util.ArrayList;
import java.util.List;

/** Normalized Gini inequality (0 equal, 1 maximal), including empty groups; zero for zero total mass.
 * Requires nonnegative finite edge weights; signed weights are rejected. */
public class GroupWeightGini<U> extends AbstractEdgePartitionMetric<U>
{
    @Override
    protected EdgePartitionStatistics.Detail statisticsDetail()
    {
        return EdgePartitionStatistics.Detail.EDGES;
    }

    @Override
    protected double evaluate(EdgePartitionStatistics<U> s)
    {
        List<Double> values = new ArrayList<>();
        for (int group = 0; group < s.edges.size(); group++)
        {
            double sum = 0.0;
            for (es.uam.eps.ir.relison.sna.edge.Edge<U> edge : s.edges.get(group))
            {
                double weight = s.weight(edge);
                if (weight < 0.0) throw new IllegalArgumentException("Group weight Gini requires nonnegative edge weights.");
                sum += weight;
            }
            if (!Double.isFinite(sum)) throw new IllegalArgumentException("Group weight exceeds the finite numeric range.");
            values.add(sum);
        }
        if (values.size() <= 1) return 0.0;
        double maximum = values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        if (maximum == 0.0) return 0.0;
        // Scaling preserves Gini and avoids overflow when summing large group weights.
        values.replaceAll(value -> value / maximum);
        return new GiniIndex().compute(values, true);
    }
}
