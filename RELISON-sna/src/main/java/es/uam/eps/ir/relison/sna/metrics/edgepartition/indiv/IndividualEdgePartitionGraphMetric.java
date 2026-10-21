package es.uam.eps.ir.relison.sna.metrics.edgepartition.indiv;

import es.uam.eps.ir.relison.sna.edge.graph.EdgePartitionSubgraphGenerator;
import es.uam.eps.ir.relison.sna.metrics.GraphMetric;
import es.uam.eps.ir.relison.sna.metrics.edgepartition.AbstractIndividualEdgePartitionMetric;
import es.uam.eps.ir.relison.sna.metrics.edgepartition.EdgePartitionStatistics;
import java.util.Objects;
import java.util.function.Supplier;

/** Applies a fresh graph metric to each group's edges and endpoints, excluding edges from every other group. */
public class IndividualEdgePartitionGraphMetric<U> extends AbstractIndividualEdgePartitionMetric<U>
{
    private final Supplier<GraphMetric<U>> metricSupplier;

    public IndividualEdgePartitionGraphMetric(Supplier<GraphMetric<U>> metricSupplier)
    {
        this.metricSupplier = Objects.requireNonNull(metricSupplier);
    }

    @Override
    protected EdgePartitionStatistics.Detail statisticsDetail()
    {
        return EdgePartitionStatistics.Detail.EDGES;
    }

    @Override
    protected double evaluate(EdgePartitionStatistics<U> s, int group)
    {
        EdgePartitionSubgraphGenerator<U> generator = new EdgePartitionSubgraphGenerator<>();
        generator.configure(s.graph, s.edges.get(group));
        es.uam.eps.ir.relison.graph.Graph<U> subgraph;
        try { subgraph = generator.generate(); }
        catch (java.util.concurrent.CancellationException e) { throw e; }
        catch (Exception e) { throw new IllegalStateException("Could not create edge-group subgraph " + group, e); }
        GraphMetric<U> metric = metricSupplier.get();
        try { return metric.compute(subgraph); }
        catch (RuntimeException e)
        {
            // Some existing graph metrics have no empty-graph value. Preserve
            // defined values such as edge count zero, and mark undefined ones.
            if (subgraph.getVertexCount() == 0 && !(e instanceof java.util.concurrent.CancellationException))
                return Double.NaN;
            throw e;
        }
    }
}
