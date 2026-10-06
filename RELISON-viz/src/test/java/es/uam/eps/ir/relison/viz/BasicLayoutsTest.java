/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz;

import es.uam.eps.ir.relison.utils.datatypes.Pair;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.fast.FastDirectedUnweightedGraph;
import es.uam.eps.ir.relison.graph.fast.FastUndirectedWeightedGraph;
import es.uam.eps.ir.relison.graph.multigraph.fast.FastDirectedWeightedMultiGraph;
import es.uam.eps.ir.relison.viz.internal.IndexedGraphSnapshot;
import es.uam.eps.ir.relison.viz.layouts.*;
import es.uam.eps.ir.relison.viz.transforms.CoordinateTransform;
import org.junit.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import static org.junit.Assert.*;

/** Invariants and reference cases for the Stage A API and layouts. */
public class BasicLayoutsTest
{
    private static final double EPSILON = 1e-10;

    private Graph<Integer> graph(int count)
    {
        Graph<Integer> graph = new FastDirectedUnweightedGraph<>();
        for (int i = 0; i < count; i++) graph.addNode(i);
        return graph;
    }

    private List<Layout<Integer>> layouts(List<Integer> nodes)
    {
        return Arrays.asList(new PresetLayout<>(), new RandomLayout<>(), new GridLayout<>(),
            new CircularLayout<>(), new ShellLayout<>(nodes.isEmpty() ? Collections.emptyList() : Collections.singletonList(nodes), 1),
            new ConcentricLayout<>(Integer::doubleValue));
    }

    private Map<Integer, Pair<Double>> initial(List<Integer> nodes)
    {
        Map<Integer, Pair<Double>> positions = new LinkedHashMap<>();
        for (int node : nodes) positions.put(node, new Pair<>((double) node, (double) -node));
        return positions;
    }

    private double radius(Pair<Double> point) { return Math.hypot(point.v1(), point.v2()); }

    @Test
    public void everyLayoutHandlesEmptyGraphs()
    {
        for (Layout<Integer> layout : layouts(Collections.emptyList()))
        {
            LayoutResult<Integer> result = layout.compute(graph(0));
            assertTrue(result.getPositions().isEmpty());
            assertEquals(0, result.getBounds().getMinX(), 0);
            assertEquals(0, result.getBounds().getMaxY(), 0);
            assertEquals(LayoutDiagnostics.Termination.COMPLETED, result.getDiagnostics().getTermination());
        }
    }

    @Test
    public void everyLayoutPlacesAllNodesWithoutChangingGraph()
    {
        Graph<Integer> graph = graph(4);
        graph.addEdge(0, 1);
        graph.addEdge(1, 2);
        graph.addEdge(2, 0);
        graph.addEdge(0, 0);
        List<Integer> nodes = Arrays.asList(3, 2, 1, 0);
        LayoutRequest<Integer> request = LayoutRequest.<Integer>builder().nodeOrder(nodes).initialPositions(initial(nodes)).build();
        for (Layout<Integer> layout : layouts(nodes))
        {
            LayoutResult<Integer> result = layout.compute(graph, request);
            assertEquals(nodes, new ArrayList<>(result.getPositions().keySet()));
            result.getPositions().values().forEach(point -> {
                assertTrue(Double.isFinite(point.v1()));
                assertTrue(Double.isFinite(point.v2()));
            });
            assertEquals(layout.getDescriptor().getId(), result.getDiagnostics().getAlgorithmId());
            assertEquals(4, graph.getVertexCount());
            assertEquals(4, graph.getEdgeCount());
            assertEquals(Arrays.asList(0, 1), graph.getAdjacentNodes(0).sorted().collect(Collectors.toList()));
        }
    }

    @Test
    public void everyLayoutHandlesOneNodeAndPreservesItsPin()
    {
        Pair<Double> pinned = new Pair<>(12.0, -7.0);
        LayoutRequest<Integer> request = LayoutRequest.<Integer>builder()
            .initialPositions(Collections.singletonMap(0, pinned)).pinnedNodes(Collections.singleton(0)).build();
        for (Layout<Integer> layout : layouts(Collections.singletonList(0)))
            assertEquals(pinned, layout.compute(graph(1), request).getPositions().get(0));
    }

    @Test
    public void circularReferenceCoordinatesAndSingleton()
    {
        Map<Integer, Pair<Double>> positions = new CircularLayout<Integer>(2, 0).compute(graph(4)).getPositions();
        assertEquals(2, positions.get(0).v1(), EPSILON);
        assertEquals(2, positions.get(1).v2(), EPSILON);
        assertEquals(-2, positions.get(2).v1(), EPSILON);
        assertEquals(-2, positions.get(3).v2(), EPSILON);
        assertEquals(new Pair<>(0.0, 0.0), new CircularLayout<Integer>().compute(graph(1)).getPositions().get(0));
    }

    @Test
    public void circleRespectsStartAngle()
    {
        Pair<Double> point = new CircularLayout<Integer>(3, Math.PI / 2).compute(graph(2)).getPositions().get(0);
        assertEquals(0, point.v1(), EPSILON);
        assertEquals(3, point.v2(), EPSILON);
    }

    @Test
    public void gridUsesSpecifiedColumnsAndSpacing()
    {
        Map<Integer, Pair<Double>> positions = new GridLayout<Integer>(2, 2).compute(graph(4)).getPositions();
        assertEquals(new Pair<>(-1.0, -1.0), positions.get(0));
        assertEquals(new Pair<>(1.0, -1.0), positions.get(1));
        assertEquals(new Pair<>(-1.0, 1.0), positions.get(2));
        assertEquals(new Pair<>(1.0, 1.0), positions.get(3));
    }

    @Test
    public void automaticGridAndWideSingletonStayCompact()
    {
        LayoutResult<Integer> result = new GridLayout<Integer>().compute(graph(5));
        assertEquals(2, result.getBounds().getMaxX() - result.getBounds().getMinX(), EPSILON);
        assertEquals(1, result.getBounds().getMaxY() - result.getBounds().getMinY(), EPSILON);
        assertEquals(new Pair<>(0.0, 0.0), new GridLayout<Integer>(100, 2).compute(graph(1)).getPositions().get(0));
    }

    @Test
    public void randomLayoutIsSeededAndBounded()
    {
        Graph<Integer> graph = graph(20);
        Layout<Integer> layout = new RandomLayout<>(4, 6);
        LayoutRequest<Integer> first = LayoutRequest.<Integer>builder().seed(47).build();
        LayoutRequest<Integer> second = LayoutRequest.<Integer>builder().seed(48).build();
        Map<Integer, Pair<Double>> positions = layout.compute(graph, first).getPositions();
        assertEquals(positions, layout.compute(graph, first).getPositions());
        assertNotEquals(positions, layout.compute(graph, second).getPositions());
        positions.values().forEach(point -> {
            assertTrue(point.v1() >= -2 && point.v1() < 2);
            assertTrue(point.v2() >= -3 && point.v2() < 3);
        });
    }

    @Test
    public void explicitOrderMakesDifferentInsertionOrdersReproducible()
    {
        Graph<Integer> first = graph(4);
        Graph<Integer> second = graph(0);
        for (int node : Arrays.asList(3, 1, 0, 2)) second.addNode(node);
        List<Integer> order = Arrays.asList(2, 0, 3, 1);
        LayoutRequest<Integer> request = LayoutRequest.<Integer>builder().nodeOrder(order).seed(12).build();
        for (Layout<Integer> layout : Arrays.<Layout<Integer>>asList(new RandomLayout<>(), new CircularLayout<>(), new GridLayout<>()))
            assertEquals(layout.compute(first, request).getPositions(), layout.compute(second, request).getPositions());
    }

    @Test
    public void presetRestoresCoordinatesInRequestedOrder()
    {
        Map<Integer, Pair<Double>> positions = initial(Arrays.asList(0, 1, 2));
        LayoutRequest<Integer> request = LayoutRequest.<Integer>builder()
            .initialPositions(positions).nodeOrder(Arrays.asList(2, 0, 1)).build();
        LayoutResult<Integer> result = new PresetLayout<Integer>().compute(graph(3), request);
        assertEquals(positions, result.getPositions());
        assertEquals(Arrays.asList(2, 0, 1), new ArrayList<>(result.getPositions().keySet()));
    }

    @Test
    public void shellUsesMembershipButRequestOrderWithinRings()
    {
        Layout<Integer> layout = new ShellLayout<>(Arrays.asList(Collections.singletonList(0), Arrays.asList(1, 2, 3)), 2);
        LayoutRequest<Integer> request = LayoutRequest.<Integer>builder().nodeOrder(Arrays.asList(3, 2, 1, 0)).build();
        Map<Integer, Pair<Double>> positions = layout.compute(graph(4), request).getPositions();
        assertEquals(0, radius(positions.get(0)), EPSILON);
        assertEquals(new Pair<>(2.0, 0.0), positions.get(3));
        assertEquals(2, radius(positions.get(1)), EPSILON);
        assertEquals(2, radius(positions.get(2)), EPSILON);
    }

    @Test
    public void shellWithMultipleInnerNodesHasPositiveRadius()
    {
        Map<Integer, Pair<Double>> positions = new ShellLayout<Integer>(
            Arrays.asList(Arrays.asList(0, 1), Collections.singletonList(2)), 3).compute(graph(3)).getPositions();
        assertEquals(3, radius(positions.get(0)), EPSILON);
        assertEquals(6, radius(positions.get(2)), EPSILON);
    }

    @Test
    public void concentricGroupsScoresDescendingAndEvaluatesEachOnce()
    {
        AtomicInteger calls = new AtomicInteger();
        Layout<Integer> layout = new ConcentricLayout<>(node -> {
            calls.incrementAndGet();
            return node == 0 ? 5 : node < 3 ? 2 : 1;
        }, 3);
        Map<Integer, Pair<Double>> positions = layout.compute(graph(4)).getPositions();
        assertEquals(4, calls.get());
        assertEquals(0, radius(positions.get(0)), EPSILON);
        assertEquals(3, radius(positions.get(1)), EPSILON);
        assertEquals(3, radius(positions.get(2)), EPSILON);
        assertEquals(6, radius(positions.get(3)), EPSILON);
    }

    @Test
    public void concentricTreatsSignedZeroAsSameScore()
    {
        Map<Integer, Pair<Double>> positions = new ConcentricLayout<Integer>(node -> node == 0 ? -0.0 : 0.0)
            .compute(graph(2)).getPositions();
        assertEquals(1, radius(positions.get(0)), EPSILON);
        assertEquals(1, radius(positions.get(1)), EPSILON);
    }

    @Test
    public void requestsAndResultsAreDefensiveCopies()
    {
        Map<Integer, Pair<Double>> input = initial(Arrays.asList(0, 1));
        List<Integer> order = new ArrayList<>(Arrays.asList(1, 0));
        Set<Integer> pins = new HashSet<>(Collections.singleton(0));
        LayoutRequest.Builder<Integer> builder = LayoutRequest.<Integer>builder()
            .initialPositions(input).nodeOrder(order).pinnedNodes(pins);
        input.clear(); order.clear(); pins.clear();
        LayoutRequest<Integer> request = builder.build();
        assertEquals(2, request.getInitialPositions().size());
        assertEquals(Arrays.asList(1, 0), request.getNodeOrder().get());
        assertEquals(Collections.singleton(0), request.getPinnedNodes());
        Map<Integer, Pair<Double>> coordinates = initial(Arrays.asList(0, 1));
        LayoutResult<Integer> result = new LayoutResult<>(coordinates,
            new LayoutDiagnostics("test", LayoutDiagnostics.Termination.COMPLETED));
        coordinates.clear();
        assertEquals(2, result.getPositions().size());
        assertThrows(UnsupportedOperationException.class, () -> result.getPositions().clear());
        assertThrows(UnsupportedOperationException.class, () -> request.getInitialPositions().clear());
        assertThrows(UnsupportedOperationException.class, () -> request.getNodeOrder().get().clear());
        assertThrows(UnsupportedOperationException.class, () -> request.getPinnedNodes().clear());
    }

    @Test
    public void snapshotPreservesDirectedParallelWeightsAndSelfLoops()
    {
        Graph<Integer> graph = new FastDirectedWeightedMultiGraph<>();
        graph.addEdge(0, 1, 2.0);
        graph.addEdge(0, 1, 4.0);
        graph.addEdge(1, 1, 3.0);
        graph.addNode(2);
        IndexedGraphSnapshot<Integer> snapshot = IndexedGraphSnapshot.capture(graph,
            LayoutRequest.<Integer>builder().nodeOrder(Arrays.asList(0, 1, 2)).build());
        assertTrue(snapshot.isDirected());
        assertArrayEquals(new int[]{1, 1}, snapshot.getTargets(0));
        assertArrayEquals(new double[]{2, 4}, snapshot.getWeights(0), EPSILON);
        assertArrayEquals(new int[]{1}, snapshot.getTargets(1));
        assertArrayEquals(new double[]{3}, snapshot.getWeights(1), EPSILON);
        assertArrayEquals(new int[0], snapshot.getTargets(2));
        snapshot.getTargets(0)[0] = 99;
        snapshot.getWeights(0)[0] = 99;
        graph.addEdge(0, 2, 5.0);
        graph.addNode(3);
        assertArrayEquals(new int[]{1, 1}, snapshot.getTargets(0));
        assertArrayEquals(new double[]{2, 4}, snapshot.getWeights(0), EPSILON);
        assertEquals(Arrays.asList(0, 1, 2), snapshot.getNodes());
        assertEquals(-1, snapshot.indexOf(3));
    }

    @Test
    public void snapshotPreservesBothDirectionsOfUndirectedEdges()
    {
        Graph<Integer> graph = new FastUndirectedWeightedGraph<>();
        graph.addEdge(0, 1, 7.0);
        IndexedGraphSnapshot<Integer> snapshot = IndexedGraphSnapshot.capture(graph,
            LayoutRequest.<Integer>builder().nodeOrder(Arrays.asList(0, 1)).build());
        assertFalse(snapshot.isDirected());
        assertArrayEquals(new int[]{1}, snapshot.getTargets(0));
        assertArrayEquals(new int[]{0}, snapshot.getTargets(1));
        assertArrayEquals(new double[]{7}, snapshot.getWeights(1), EPSILON);
        for (Layout<Integer> layout : layouts(Arrays.asList(0, 1)))
            assertEquals(2, layout.compute(graph, LayoutRequest.<Integer>builder().initialPositions(initial(Arrays.asList(0, 1))).build()).getPositions().size());
    }

    @Test
    public void transformScalesThenTranslatesAndRecomputesBounds()
    {
        Layout<Integer> pipeline = new LayoutPipeline<>(new GridLayout<>(2, 2),
            Collections.singletonList(new CoordinateTransform<>(2, -3, 10, 20)));
        LayoutResult<Integer> result = pipeline.compute(graph(4));
        assertEquals(new Pair<>(8.0, 23.0), result.getPositions().get(0));
        assertEquals(new Pair<>(12.0, 17.0), result.getPositions().get(3));
        assertEquals(8, result.getBounds().getMinX(), EPSILON);
        assertEquals(23, result.getBounds().getMaxY(), EPSILON);
        assertEquals("grid", result.getDiagnostics().getAlgorithmId());
    }

    @Test
    public void transformPreservesPinsExactly()
    {
        Pair<Double> pin = new Pair<>(100.0, -100.0);
        LayoutRequest<Integer> request = LayoutRequest.<Integer>builder()
            .initialPositions(Collections.singletonMap(0, pin)).pinnedNodes(Collections.singleton(0)).build();
        Layout<Integer> pipeline = new LayoutPipeline<>(new CircularLayout<>(),
            Arrays.asList(new CoordinateTransform<>(2, 2, 5, 5), new CoordinateTransform<>(1, 1, 10, 10)));
        LayoutResult<Integer> result = pipeline.compute(graph(2), request);
        assertEquals(pin, result.getPositions().get(0));
        assertEquals(13, result.getPositions().get(1).v1(), EPSILON);
        assertEquals(100, result.getBounds().getMaxX(), EPSILON);
    }

    @Test
    public void pipelineRejectsLostNodesOrMovedPins()
    {
        LayoutPostProcessor<Integer> dropping = (result, request) -> new LayoutResult<>(Collections.emptyMap(), result.getDiagnostics());
        Layout<Integer> pipeline = new LayoutPipeline<>(new GridLayout<>(), Collections.singletonList(dropping));
        assertThrows(IllegalStateException.class, () -> pipeline.compute(graph(2)));
        LayoutRequest<Integer> request = LayoutRequest.<Integer>builder()
            .initialPositions(Collections.singletonMap(0, new Pair<>(8.0, 8.0))).pinnedNodes(Collections.singleton(0)).build();
        LayoutPostProcessor<Integer> moving = (result, inputs) -> new LayoutResult<>(
            Collections.singletonMap(0, new Pair<>(0.0, 0.0)), result.getDiagnostics());
        Layout<Integer> movingPipeline = new LayoutPipeline<>(new GridLayout<>(), Collections.singletonList(moving));
        assertThrows(IllegalStateException.class, () -> movingPipeline.compute(graph(1), request));
    }

    @Test
    public void invalidRequestsFailClearly()
    {
        assertThrows(IllegalArgumentException.class, () -> LayoutRequest.<Integer>builder().pinnedNodes(Collections.singleton(0)).build());
        assertThrows(IllegalArgumentException.class, () -> LayoutRequest.<Integer>builder().nodeOrder(Arrays.asList(0, 0)).build());
        assertThrows(IllegalArgumentException.class, () -> LayoutRequest.<Integer>builder().nodeOrder(Arrays.asList(0, null)).build());
        assertThrows(IllegalArgumentException.class, () -> new GridLayout<Integer>().compute(graph(2),
            LayoutRequest.<Integer>builder().nodeOrder(Collections.singletonList(0)).build()));
        assertThrows(IllegalArgumentException.class, () -> new GridLayout<Integer>().compute(graph(2),
            LayoutRequest.<Integer>builder().nodeOrder(Arrays.asList(0, 2)).build()));
        assertThrows(IllegalArgumentException.class, () -> new GridLayout<Integer>().compute(graph(2),
            LayoutRequest.<Integer>builder().initialPositions(Collections.singletonMap(3, new Pair<>(0.0, 0.0))).build()));
    }

    @Test
    public void missingPresetAndInvalidPartitionsAreRejected()
    {
        assertThrows(IllegalArgumentException.class, () -> new PresetLayout<Integer>().compute(graph(1)));
        assertThrows(IllegalArgumentException.class, () -> new ShellLayout<Integer>(Arrays.asList(Arrays.asList(0, 1), Collections.singletonList(1)), 1));
        assertThrows(IllegalArgumentException.class, () -> new ShellLayout<Integer>(Collections.singletonList(Collections.emptyList()), 1));
        assertThrows(IllegalArgumentException.class, () -> new ShellLayout<Integer>(Collections.singletonList(Collections.singletonList(0)), 1).compute(graph(2)));
        assertThrows(IllegalArgumentException.class, () -> new ShellLayout<Integer>(Collections.singletonList(Arrays.asList(0, 2)), 1).compute(graph(2)));
    }

    @Test
    public void nonFiniteCoordinatesScoresAndParametersAreRejected()
    {
        assertThrows(IllegalArgumentException.class, () -> Bounds2D.of(Collections.singletonList(new Pair<>(Double.NaN, 0.0))));
        assertThrows(IllegalArgumentException.class, () -> LayoutRequest.builder().initialPositions(Collections.singletonMap(0, new Pair<>(0.0, Double.POSITIVE_INFINITY))).build());
        assertThrows(IllegalArgumentException.class, () -> new CircularLayout<>(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new CircularLayout<>(1, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new RandomLayout<>(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> new GridLayout<>(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> new GridLayout<>(0, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new ConcentricLayout<Integer>(node -> Double.NaN).compute(graph(1)));
        assertThrows(IllegalArgumentException.class, () -> new CoordinateTransform<>(1, 1, Double.NaN, 0));
    }

    @Test
    public void overflowFailsInsteadOfReturningInfiniteCoordinates()
    {
        Layout<Integer> pipeline = new LayoutPipeline<>(new CircularLayout<>(Double.MAX_VALUE, 0),
            Collections.singletonList(new CoordinateTransform<>(2, 2, 0, 0)));
        assertThrows(IllegalArgumentException.class, () -> pipeline.compute(graph(2)));
    }
}
