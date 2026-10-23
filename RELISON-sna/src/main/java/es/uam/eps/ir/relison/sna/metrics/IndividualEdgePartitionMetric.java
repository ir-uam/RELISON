package es.uam.eps.ir.relison.sna.metrics;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.sna.edge.EdgePartition;
import java.util.Map;

/** Metric computed independently for each group in an edge partition. */
public interface IndividualEdgePartitionMetric<U>
{
    double compute(Graph<U> graph, EdgePartition<U> partition, int group);
    Map<Integer, Double> compute(Graph<U> graph, EdgePartition<U> partition);
    double averageValue(Graph<U> graph, EdgePartition<U> partition);
}
