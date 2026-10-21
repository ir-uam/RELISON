package es.uam.eps.ir.relison.sna.edge.detection;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.utils.ComputationMonitor;
import es.uam.eps.ir.relison.graph.multigraph.MultiGraph;
import es.uam.eps.ir.relison.sna.edge.Edge;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Shared graph-edge enumeration for edge partition detectors. */
public final class EdgePartitionSupport
{
    private EdgePartitionSupport() { }

    public static <U> void forEachEdge(Graph<U> graph, Consumer<Edge<U>> consumer)
    {
        ComputationMonitor monitor = ComputationMonitor.current();
        monitor.progress("Processing edge partition membership", 0, graph.getEdgeCount(), "edges");
        long[] done = {0};
        Consumer<Edge<U>> tracked = monitor == ComputationMonitor.NONE ? consumer : edge -> {
            monitor.checkCancelled(); consumer.accept(edge);
            long count = ++done[0];
            if (count == 1 || (count & 255) == 0 || count == graph.getEdgeCount())
                monitor.progress("Processing edge partition membership", count, graph.getEdgeCount(), "edges");
        };
        Set<Edge<U>> seen = graph.isDirected() ? null : new HashSet<>();
        if (graph instanceof MultiGraph<?>)
        {
            MultiGraph<U> multiGraph = (MultiGraph<U>) graph;
            graph.getAllNodes().forEach(source -> multiGraph.getAdjacentNodesWeightsLists(source).forEach(edges ->
            {
                U target = edges.getIdx();
                List<Long> ids = multiGraph.getEdgeIds(source, target);
                List<Double> weights = edges.getValue();
                for (int index = 0; index < weights.size(); index++)
                {
                    Long id = index < ids.size() ? ids.get(index) : null;
                    Edge<U> edge = new Edge<>(source, target, index, id, graph.isDirected());
                    if (seen == null || seen.add(edge)) tracked.accept(edge);
                }
            }));
        }
        else
        {
            graph.getAllNodes().forEach(source -> graph.getAdjacentNodes(source).forEach(target ->
            {
                Edge<U> edge = new Edge<>(source, target, graph.isDirected());
                if (seen == null || seen.add(edge)) tracked.accept(edge);
            }));
        }
    }
}
