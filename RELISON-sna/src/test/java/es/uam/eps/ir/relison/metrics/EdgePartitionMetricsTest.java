package es.uam.eps.ir.relison.metrics;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.graph.fast.FastDirectedUnweightedGraph;
import es.uam.eps.ir.relison.graph.multigraph.fast.FastDirectedWeightedMultiGraph;
import es.uam.eps.ir.relison.sna.edge.EdgePartition;
import es.uam.eps.ir.relison.sna.edge.detection.EdgePartitionSupport;
import es.uam.eps.ir.relison.sna.metrics.edgepartition.EdgePartitionStatistics;
import es.uam.eps.ir.relison.sna.metrics.edgepartition.graph.*;
import es.uam.eps.ir.relison.sna.metrics.edgepartition.indiv.*;
import java.lang.reflect.Proxy;
import org.junit.Test;
import static org.junit.Assert.*;

/** Guards against reintroducing whole-network preprocessing for count metrics. */
public class EdgePartitionMetricsTest
{
    private static final double EPS = 1e-12;

    @SuppressWarnings("unchecked")
    private Graph<String> countOnlyGraph(long edgeCount)
    {
        return (Graph<String>) Proxy.newProxyInstance(Graph.class.getClassLoader(), new Class<?>[]{Graph.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getEdgeCount")) return edgeCount;
                    throw new AssertionError("Count metric must not inspect graph structure: " + method.getName());
                });
    }

    @Test
    public void countMetricsUseStoredSizesWithoutTraversingGraph()
    {
        Graph<String> graph = new FastDirectedUnweightedGraph<>();
        graph.addEdge("a", "b"); graph.addEdge("b", "c");
        graph.addEdge("c", "a"); graph.addEdge("d", "e");
        EdgePartition<String> partition = new EdgePartition<>();
        partition.addPartition(); partition.addPartition(); partition.addPartition();
        EdgePartitionSupport.forEachEdge(graph, edge -> {
            if (edge.getSource().equals("d")) return;
            partition.add(edge, edge.getSource().equals("c") ? 1 : 0);
        });
        Graph<String> counts = countOnlyGraph(4);
        assertEquals(2.0, new Size<String>().compute(counts, partition, 0), EPS);
        assertEquals(0.5, new EdgeShare<String>().compute(counts, partition, 0), EPS);
        assertEquals(0.25, new EdgeShare<String>().averageValue(counts, partition), EPS);
        assertEquals(0.25, new EdgeShare<String>().compute(counts, partition).get(1), EPS);
        assertEquals(3, new Size<String>().compute(counts, partition).size());
        assertEquals(3.0, new NumberOfGroups<String>().compute(counts, partition), EPS);
        assertEquals(2.0, new NumberOfGroups<String>(true).compute(counts, partition), EPS);
        assertEquals(0.75, new AssignmentCoverage<String>().compute(counts, partition), EPS);
        assertEquals(2.0 / 3.0, new GroupSizeGini<String>().compute(counts, partition), EPS);
        assertEquals(0.9182958340544896, new GroupSizeEntropy<String>().compute(counts, partition), EPS);
    }

    @Test
    public void emptyCountMetricsRemainZero()
    {
        EdgePartition<String> partition = new EdgePartition<>();
        partition.addPartition(); partition.addPartition();
        Graph<String> counts = countOnlyGraph(0);
        assertEquals(0.0, new EdgeShare<String>().compute(counts, partition, 0), EPS);
        assertEquals(0.0, new EdgeShare<String>().averageValue(counts, partition), EPS);
        assertEquals(0.0, new AssignmentCoverage<String>().compute(counts, partition), EPS);
        assertEquals(0.0, new GroupSizeGini<String>().compute(counts, partition), EPS);
        assertEquals(0.0, new GroupSizeEntropy<String>().compute(counts, partition), EPS);
        assertThrows(IllegalArgumentException.class, () -> new Size<String>().compute(counts, partition, -1));
        assertThrows(IllegalArgumentException.class, () -> new EdgeShare<String>().compute(counts, partition, 2));
    }

    @Test
    public void minimalSnapshotsPreserveWeightsLoopsParallelEdgesAndUnassignedNodes()
    {
        Graph<String> graph = new FastDirectedWeightedMultiGraph<>();
        graph.addEdge("a", "b", 2.0); graph.addEdge("a", "b", 3.0);
        graph.addEdge("b", "c", 4.0); graph.addEdge("c", "c", 6.0);
        graph.addEdge("d", "e", 5.0);
        EdgePartition<String> partition = new EdgePartition<>();
        partition.addPartition(); partition.addPartition(); partition.addPartition();
        EdgePartitionSupport.forEachEdge(graph, edge -> {
            if (!edge.getSource().equals("d")) partition.add(edge, edge.getSource().equals("a") ? 0 : 1);
        });
        EdgePartitionStatistics<String> edges = new EdgePartitionStatistics<>(graph, partition, EdgePartitionStatistics.Detail.EDGES);
        assertTrue(edges.nodes.isEmpty()); assertTrue(edges.nodeGroups.isEmpty());
        EdgePartitionStatistics<String> nodes = new EdgePartitionStatistics<>(graph, partition, EdgePartitionStatistics.Detail.NODES);
        assertEquals(2, nodes.nodes.get(0).size()); assertTrue(nodes.nodeGroups.isEmpty());
        assertEquals(5.0, new TotalWeight<String>().compute(graph, partition, 0), EPS);
        assertEquals(10.0, new TotalWeight<String>().compute(graph, partition).get(1), EPS);
        assertEquals(2.0, new NodeCoverage<String>().compute(graph, partition, 0), EPS);
        assertEquals(1.0, new NodeCoverage<String>(EdgeOrientation.OUT).compute(graph, partition, 0), EPS);
        assertEquals(1.0, new NodeCoverage<String>(EdgeOrientation.IN).compute(graph, partition, 1), EPS);
        assertEquals(1.0, new SharedNodes<String>().compute(graph, partition, 0), EPS);
        assertEquals(0.5, new SharedNodeRatio<String>().compute(graph, partition, 1), EPS);
        assertEquals(1.0, new GroupDegree<String>().compute(graph, partition, 0), EPS);
        assertEquals(1.0, new Components<String>().compute(graph, partition, 1), EPS);
        assertEquals(1.0, new LargestComponentShare<String>().compute(graph, partition, 1), EPS);
        assertEquals(0.2, new NodeMixingRatio<String>().compute(graph, partition), EPS);
        assertEquals(0.8, new MeanNodeParticipation<String>().compute(graph, partition), EPS);
    }
}
