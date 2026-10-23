package es.uam.eps.ir.relison.gui;

import es.uam.eps.ir.relison.utils.ComputationMonitor;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.fast.FastDirectedWeightedGraph;
import es.uam.eps.ir.relison.graph.fast.FastUndirectedWeightedGraph;
import es.uam.eps.ir.relison.graph.multigraph.fast.FastDirectedWeightedMultiGraph;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.sna.metrics.*;
import es.uam.eps.ir.relison.sna.metrics.distance.CompleteDistanceCalculator;
import es.uam.eps.ir.relison.sna.metrics.vertex.*;
import es.uam.eps.ir.relison.sna.metrics.pair.*;
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.sna.metrics.communities.indiv.BoundaryNodes;
import es.uam.eps.ir.relison.sna.metrics.communities.indiv.IndividualCommunityGraphMetric;
import es.uam.eps.ir.relison.sna.metrics.graph.NumEdges;
import es.uam.eps.ir.relison.diffusion.protocols.SimpleProtocol;
import es.uam.eps.ir.relison.diffusion.simulation.*;
import es.uam.eps.ir.relison.diffusion.stop.NumIterStopCondition;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class ProgressMonitoringTest
{
    private static class Recording implements ComputationMonitor {
        String phase, unit; long done, total;
        @Override public void progress(String phase, long done, long total, String unit) {
            this.phase = phase; this.done = done; this.total = total; this.unit = unit;
        }
    }
    private Graph<String> graph(boolean directed) {
        Graph<String> graph = directed ? new FastDirectedWeightedGraph<>() : new FastUndirectedWeightedGraph<>();
        graph.addEdge("a", "b", 2); graph.addEdge("b", "c", 3); graph.addEdge("c", "a", 4); graph.addEdge("a", "a", 1);
        return graph;
    }
    @Test public void nodeResultsAreIdenticalAndScopesRestoreThePreviousMonitor() {
        Graph<String> graph = graph(true);
        List<VertexMetric<String>> metrics = List.of(new Degree<>(EdgeOrientation.OUT), new PageRank<>(.85),
                new Coreness<>(EdgeOrientation.OUT), new HITS<>(true), new FreeDiscovery<>(EdgeOrientation.OUT));
        for (VertexMetric<String> metric : metrics) {
            Map<String, Double> expected = new HashMap<>(metric.compute(graph));
            Recording monitor = new Recording();
            try (ComputationMonitor.Scope scope = ComputationMonitor.open(monitor)) {
                assertEquals(expected, metric.compute(graph));
                assertTrue("Expected progress for " + metric.getClass(), monitor.done > 0);
            }
            assertSame(ComputationMonitor.NONE, ComputationMonitor.current());
        }
    }
    @Test public void linkProgressPreservesDirectionSelfLoopsAndParallelEdgeSemantics() {
        for (Graph<String> graph : List.of(graph(true), graph(false), new FastDirectedWeightedMultiGraph<String>())) {
            if (graph.isMultigraph()) { graph.addEdge("a", "b", 1); graph.addEdge("a", "b", 2); }
            List<PairMetric<String>> metrics = List.of(new EdgeWeight<>(), new ReciprocityRate<>(),
                    new PreferentialAttachment<>(EdgeOrientation.OUT, EdgeOrientation.IN),
                    new NeighbourOverlap<>(EdgeOrientation.OUT, EdgeOrientation.IN));
            for (PairMetric<String> metric : metrics) {
                Map<?, ?> expected = metric.computeOnlyLinks(graph);
                Recording monitor = new Recording();
                try (ComputationMonitor.Scope scope = ComputationMonitor.open(monitor)) {
                    assertEquals(expected, metric.computeOnlyLinks(graph));
                    assertEquals(expected.size(), monitor.total);
                    assertEquals(expected.size(), monitor.done);
                }
            }
        }
    }
    @Test public void cancelledShortestPathsAreRecomputedAndNeverTreatedAsCached() {
        Graph<String> graph = graph(true);
        CompleteDistanceCalculator<String> calculator = new CompleteDistanceCalculator<>();
        ComputationMonitor cancel = (phase, done, total, unit) -> { if (unit.equals("source nodes") && done == 1) throw new java.util.concurrent.CancellationException(); };
        try (ComputationMonitor.Scope scope = ComputationMonitor.open(cancel)) {
            try { calculator.computeDistances(graph); fail(); } catch (java.util.concurrent.CancellationException expected) { }
        }
        Recording monitor = new Recording();
        try (ComputationMonitor.Scope scope = ComputationMonitor.open(monitor)) { assertTrue(calculator.computeDistances(graph)); }
        CompleteDistanceCalculator<String> reference = new CompleteDistanceCalculator<>(); reference.computeDistances(graph);
        assertEquals(reference.getNodeBetweenness(), calculator.getNodeBetweenness());
        assertEquals(3, monitor.done); assertEquals(3, monitor.total); assertEquals("source nodes", monitor.unit);
    }
    @Test public void communityCountersUseOneBulkComputationAndCountFinishedGroups() {
        Graph<String> graph = graph(true); Communities<String> groups = new Communities<>();
        groups.addCommunity(); groups.addCommunity(); groups.add("a", 0); groups.add("b", 0); groups.add("c", 1);
        List<IndividualCommunityMetric<String>> metrics = List.of(new BoundaryNodes<>(), new IndividualCommunityGraphMetric<>(NumEdges::new));
        for (IndividualCommunityMetric<String> metric : metrics) {
            Map<Integer, Double> expected = metric.compute(graph, groups); Recording monitor = new Recording();
            try (ComputationMonitor.Scope scope = ComputationMonitor.open(monitor)) { assertEquals(expected, metric.compute(graph, groups)); }
            assertEquals(2, monitor.done); assertEquals(2, monitor.total); assertEquals("communities", monitor.unit);
        }
    }
    @Test public void diffusionCountsCompletedTurnsAfterConsumersIncludingIterationZero() {
        Simulator<String, String, String> simulator = new Simulator<>(new SimpleProtocol<>(1, 1), new NumIterStopCondition<>(2));
        simulator.initialize(DiffusionData.synthetic(graph(true), 1));
        Recording monitor = new Recording(); long[] consumed = {0};
        try (ComputationMonitor.Scope scope = ComputationMonitor.open(monitor)) {
            simulator.simulate(new SimulationConsumer<String, String, String>() {
                @Override public void start(int initial) { }
                @Override public void accept(Iteration<String, String, String> iteration) {
                    assertEquals(consumed[0], monitor.unit == null ? 0 : monitor.done); consumed[0]++;
                }
                @Override public void finish() { }
            });
        }
        assertEquals(3, consumed[0]); assertEquals(3, monitor.done); assertEquals(3, monitor.total); assertEquals("turns", monitor.unit);
    }
}
