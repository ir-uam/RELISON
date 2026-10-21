package es.uam.eps.ir.relison.sna.metrics;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.sna.edge.EdgePartition;

/** Global metric of an edge partition. */
public interface EdgePartitionMetric<U>
{
    double compute(Graph<U> graph, EdgePartition<U> partition);
}
