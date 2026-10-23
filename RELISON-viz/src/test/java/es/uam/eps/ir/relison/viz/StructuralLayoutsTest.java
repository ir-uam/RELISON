/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz;

import es.uam.eps.ir.relison.utils.datatypes.Pair;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.fast.FastDirectedUnweightedGraph;
import es.uam.eps.ir.relison.graph.fast.FastUndirectedUnweightedGraph;
import es.uam.eps.ir.relison.graph.multigraph.fast.FastDirectedWeightedMultiGraph;
import es.uam.eps.ir.relison.viz.layouts.*;
import es.uam.eps.ir.relison.viz.transforms.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/** Geometric invariants, invalid topology, and constraint cases for Stage B. */
public class StructuralLayoutsTest
{
    private static final double EPSILON = 1e-8;

    private Graph<Integer> directed(int size)
    {
        Graph<Integer> graph = new FastDirectedUnweightedGraph<>();
        for (int i = 0; i < size; i++) graph.addNode(i);
        return graph;
    }

    private Graph<Integer> tree()
    {
        Graph<Integer> graph = directed(7);
        graph.addEdge(0, 1); graph.addEdge(0, 2);
        graph.addEdge(1, 3); graph.addEdge(1, 4);
        graph.addEdge(2, 5); graph.addEdge(5, 6);
        return graph;
    }

    private double radius(Pair<Double> point) { return Math.hypot(point.v1(), point.v2()); }

    @Test
    public void radialUsesHopDistancesAndAnExplicitUnreachableRing()
    {
        Graph<Integer> graph = directed(5);
        graph.addEdge(1, 0); graph.addEdge(0, 2); graph.addEdge(2, 3);
        Map<Integer, Pair<Double>> positions = new RadialLayout<Integer>(0, EdgeOrientation.OUT, 2).compute(graph).getPositions();
        assertEquals(0, radius(positions.get(0)), EPSILON);
        assertEquals(2, radius(positions.get(2)), EPSILON);
        assertEquals(4, radius(positions.get(3)), EPSILON);
        assertEquals(6, radius(positions.get(1)), EPSILON);
        assertEquals(6, radius(positions.get(4)), EPSILON);
        positions = new RadialLayout<Integer>(0, EdgeOrientation.IN, 2).compute(graph).getPositions();
        assertEquals(2, radius(positions.get(1)), EPSILON);
        assertEquals(4, radius(positions.get(2)), EPSILON);
        positions = new RadialLayout<Integer>(0, EdgeOrientation.UND, 2).compute(graph).getPositions();
        assertEquals(2, radius(positions.get(1)), EPSILON);
        assertEquals(4, radius(positions.get(3)), EPSILON);
    }

    @Test
    public void mutualTraversalUsesOnlyReciprocalEdges()
    {
        Graph<Integer> graph = directed(4);
        graph.addEdge(0, 1); graph.addEdge(1, 0);
        graph.addEdge(1, 2); graph.addEdge(2, 3); graph.addEdge(3, 2);
        Map<Integer, Pair<Double>> positions = new RadialLayout<Integer>(0, EdgeOrientation.MUTUAL, 2)
            .compute(graph).getPositions();
        assertEquals(2, radius(positions.get(1)), EPSILON);
        assertEquals(4, radius(positions.get(2)), EPSILON);
        assertEquals(4, radius(positions.get(3)), EPSILON);
    }

    @Test
    public void radialHandlesCyclesAndDefaultsToFirstOrderedNode()
    {
        Graph<Integer> graph = directed(3);
        graph.addEdge(0, 1); graph.addEdge(1, 2); graph.addEdge(2, 0); graph.addEdge(0, 0);
        LayoutRequest<Integer> request = LayoutRequest.<Integer>builder().nodeOrder(Arrays.asList(2, 0, 1)).build();
        Map<Integer, Pair<Double>> positions = new RadialLayout<Integer>().compute(graph, request).getPositions();
        assertEquals(0, radius(positions.get(2)), EPSILON);
        assertEquals(1, radius(positions.get(0)), EPSILON);
        assertEquals(1, radius(positions.get(1)), EPSILON);
        assertEquals(Arrays.asList(2, 0, 1), new ArrayList<>(positions.keySet()));
    }

    @Test
    public void automaticBipartiteColoursDirectedEdgesAndIncludesIsolates()
    {
        Graph<Integer> graph = directed(6);
        graph.addEdge(0, 1); graph.addEdge(2, 1); graph.addEdge(2, 3);
        Map<Integer, Pair<Double>> positions = new BipartiteLayout<Integer>(10, 2, 4).compute(graph).getPositions();
        assertEquals(6, positions.size());
        for (int source : Arrays.asList(0, 2))
            graph.getAdjacentNodes(source).forEach(target -> assertNotEquals(positions.get(source).v1(), positions.get(target).v1(), EPSILON));
        positions.values().forEach(point -> assertEquals(5, Math.abs(point.v1()), EPSILON));
        assertEquals(3, graph.getEdgeCount());
    }

    @Test
    public void bipartiteRejectsOddCyclesAndSelfLoops()
    {
        Graph<Integer> triangle = directed(3);
        triangle.addEdge(0, 1); triangle.addEdge(1, 2); triangle.addEdge(2, 0);
        assertThrows(IllegalArgumentException.class, () -> new BipartiteLayout<Integer>().compute(triangle));
        Graph<Integer> loop = directed(1); loop.addEdge(0, 0);
        assertThrows(IllegalArgumentException.class, () -> new BipartiteLayout<Integer>().compute(loop));
    }

    @Test
    public void explicitBipartitePreservesSidesAndHasDeterministicOrdering()
    {
        Graph<Integer> graph = directed(4); graph.addEdge(0, 2); graph.addEdge(1, 3);
        Layout<Integer> layout = new BipartiteLayout<>(Arrays.asList(0, 1), Arrays.asList(2, 3), 10, 2, 0);
        LayoutRequest<Integer> request = LayoutRequest.<Integer>builder().nodeOrder(Arrays.asList(1, 0, 3, 2)).build();
        Map<Integer, Pair<Double>> positions = layout.compute(graph, request).getPositions();
        assertEquals(-5, positions.get(0).v1(), EPSILON);
        assertEquals(5, positions.get(2).v1(), EPSILON);
        assertEquals(-1, positions.get(1).v2(), EPSILON);
        assertEquals(1, positions.get(0).v2(), EPSILON);
        assertEquals(positions, layout.compute(graph, request).getPositions());
    }

    @Test
    public void multipartiteUsesOrderedColumnsAndAllowsNonAdjacentConnections()
    {
        Graph<Integer> graph = directed(4); graph.addEdge(0, 2); graph.addEdge(0, 3);
        Map<Integer, Pair<Double>> positions = new MultipartiteLayout<Integer>(
            Arrays.asList(Arrays.asList(0, 1), Collections.singletonList(2), Collections.singletonList(3)), 10, 2, 0)
            .compute(graph).getPositions();
        assertEquals(-10, positions.get(0).v1(), EPSILON);
        assertEquals(0, positions.get(2).v1(), EPSILON);
        assertEquals(10, positions.get(3).v1(), EPSILON);
        assertEquals(0, positions.get(3).v2(), EPSILON);
    }

    @Test
    public void barycentreOrderingRemovesAReferenceCrossing()
    {
        Graph<Integer> graph = directed(4); graph.addEdge(0, 3); graph.addEdge(1, 2);
        List<List<Integer>> groups = Arrays.asList(Arrays.asList(0, 1), Arrays.asList(2, 3));
        Map<Integer, Pair<Double>> plain = new MultipartiteLayout<Integer>(groups, 10, 1, 0).compute(graph).getPositions();
        Map<Integer, Pair<Double>> reduced = new MultipartiteLayout<Integer>(groups, 10, 1, 4).compute(graph).getPositions();
        assertTrue((plain.get(0).v2() - plain.get(1).v2()) * (plain.get(3).v2() - plain.get(2).v2()) < 0);
        assertTrue((reduced.get(0).v2() - reduced.get(1).v2()) * (reduced.get(3).v2() - reduced.get(2).v2()) > 0);
    }

    @Test
    public void multipartiteRejectsInvalidPartitions()
    {
        assertThrows(IllegalArgumentException.class, () -> new MultipartiteLayout<Integer>(Collections.singletonList(Collections.emptyList())));
        assertThrows(IllegalArgumentException.class, () -> new MultipartiteLayout<Integer>(Arrays.asList(Arrays.asList(0, 1), Collections.singletonList(1))));
        assertThrows(IllegalArgumentException.class, () -> new MultipartiteLayout<Integer>(Arrays.asList(Collections.singletonList(0), Collections.emptyList())).compute(directed(2)));
        Graph<Integer> graph = directed(2); graph.addEdge(0, 1);
        assertThrows(IllegalArgumentException.class, () -> new MultipartiteLayout<Integer>(Arrays.asList(Arrays.asList(0, 1), Collections.emptyList())).compute(graph));
    }

    @Test
    public void multipartiteDefensivelyCopiesSuppliedPartitions()
    {
        List<Integer> left = new ArrayList<>(Arrays.asList(0, 1));
        List<Integer> right = new ArrayList<>(Arrays.asList(2, 3));
        Layout<Integer> layout = new MultipartiteLayout<>(Arrays.asList(left, right));
        left.clear(); right.clear();
        assertEquals(4, layout.compute(directed(4)).getPositions().size());
    }

    @Test
    public void tidyTreeCentresParentsAndSeparatesNodesAtEveryLevel()
    {
        Graph<Integer> graph = tree();
        Map<Integer, Pair<Double>> positions = new TreeLayout<Integer>(0, 2, 3).compute(graph).getPositions();
        assertEquals(0, positions.get(0).v1(), EPSILON);
        assertEquals(0, positions.get(0).v2(), EPSILON);
        assertEquals(3, positions.get(1).v2(), EPSILON);
        assertEquals(9, positions.get(6).v2(), EPSILON);
        assertTidy(graph, positions, 2);
    }

    private void assertTidy(Graph<Integer> graph, Map<Integer, Pair<Double>> positions, double spacing)
    {
        Map<Double, List<Double>> levels = new HashMap<>();
        positions.values().forEach(point -> levels.computeIfAbsent(point.v2(), key -> new ArrayList<>()).add(point.v1()));
        for (List<Double> xs : levels.values())
        {
            Collections.sort(xs);
            for (int i = 1; i < xs.size(); i++) assertTrue(xs.get(i) - xs.get(i - 1) >= spacing - EPSILON);
        }
        for (int node : positions.keySet())
        {
            List<Integer> children = graph.getAdjacentNodes(node).collect(java.util.stream.Collectors.toList());
            if (children.isEmpty()) continue;
            double min = children.stream().mapToDouble(child -> positions.get(child).v1()).min().getAsDouble();
            double max = children.stream().mapToDouble(child -> positions.get(child).v1()).max().getAsDouble();
            assertEquals((min + max) / 2, positions.get(node).v1(), EPSILON);
        }
    }

    @Test
    public void tidyTreeHandlesUnevenRandomBranching()
    {
        Random random = new Random(73);
        for (int sample = 0; sample < 20; sample++)
        {
            Graph<Integer> graph = directed(100);
            for (int node = 1; node < 100; node++) graph.addEdge(random.nextInt(node), node);
            assertTidy(graph, new TreeLayout<Integer>().compute(graph).getPositions(), 1);
        }
    }

    @Test
    public void reversingSiblingOrderMirrorsTheTidyTree()
    {
        Graph<Integer> graph = tree();
        Layout<Integer> layout = new TreeLayout<>(0, 1, 1);
        Map<Integer, Pair<Double>> forward = layout.compute(graph).getPositions();
        Map<Integer, Pair<Double>> reverse = layout.compute(graph,
            LayoutRequest.<Integer>builder().nodeOrder(Arrays.asList(6, 5, 4, 3, 2, 1, 0)).build()).getPositions();
        for (int node : forward.keySet()) assertEquals(-forward.get(node).v1(), reverse.get(node).v1(), EPSILON);
    }

    @Test(timeout = 30000)
    public void deepTreesUseExplicitStacks()
    {
        Graph<Integer> graph = directed(10000);
        for (int i = 1; i < 10000; i++) graph.addEdge(i - 1, i);
        Map<Integer, Pair<Double>> positions = new TreeLayout<Integer>().compute(graph).getPositions();
        assertEquals(10000, positions.size());
        assertEquals(0, positions.get(9999).v1(), EPSILON);
        assertEquals(9999, positions.get(9999).v2(), EPSILON);
    }

    @Test
    public void undirectedTreesCanBeRerootedAndForestsIncludeIsolates()
    {
        Graph<Integer> graph = new FastUndirectedUnweightedGraph<>();
        graph.addEdge(0, 1); graph.addEdge(1, 2); graph.addNode(3);
        Map<Integer, Pair<Double>> positions = new TreeLayout<Integer>(2, 2, 5).compute(graph).getPositions();
        assertEquals(0, positions.get(2).v2(), EPSILON);
        assertEquals(5, positions.get(1).v2(), EPSILON);
        assertEquals(10, positions.get(0).v2(), EPSILON);
        assertEquals(0, positions.get(3).v2(), EPSILON);
        assertTrue(Math.abs(positions.get(2).v1() - positions.get(3).v1()) >= 2);
    }

    @Test
    public void treeRejectsCyclesMultipleParentsAndNonRootSelections()
    {
        Graph<Integer> cycle = directed(3); cycle.addEdge(0, 1); cycle.addEdge(1, 2); cycle.addEdge(2, 0);
        assertThrows(IllegalArgumentException.class, () -> new TreeLayout<Integer>().compute(cycle));
        Graph<Integer> dag = directed(3); dag.addEdge(0, 2); dag.addEdge(1, 2);
        assertThrows(IllegalArgumentException.class, () -> new TreeLayout<Integer>().compute(dag));
        assertThrows(IllegalArgumentException.class, () -> new TreeLayout<Integer>(1, 1, 1).compute(tree()));
        Graph<Integer> undirected = new FastUndirectedUnweightedGraph<>();
        undirected.addEdge(0, 1); undirected.addEdge(1, 2); undirected.addEdge(2, 0);
        assertThrows(IllegalArgumentException.class, () -> new TreeLayout<Integer>().compute(undirected));
        Graph<Integer> loop = directed(1); loop.addEdge(0, 0);
        assertThrows(IllegalArgumentException.class, () -> new TreeLayout<Integer>().compute(loop));
    }

    @Test
    public void parallelEdgesDoNotDuplicateStructuralParentsOrHopDistances()
    {
        Graph<Integer> graph = new FastDirectedWeightedMultiGraph<>();
        graph.addEdge(0, 1, 2.0); graph.addEdge(0, 1, 9.0); graph.addEdge(1, 2, 3.0);
        assertEquals(2, radius(new RadialLayout<Integer>().compute(graph).getPositions().get(2)), EPSILON);
        assertEquals(2, new TreeLayout<Integer>().compute(graph).getPositions().get(2).v2(), EPSILON);
        assertEquals(3, new BipartiteLayout<Integer>().compute(graph).getPositions().size());
    }

    @Test
    public void everyStructuralLayoutHandlesEmptyGraphsAndPins()
    {
        List<Layout<Integer>> layouts = Arrays.asList(new RadialLayout<>(), new TreeLayout<>(), new BipartiteLayout<>(),
            new MultipartiteLayout<>(Arrays.asList(Collections.emptyList(), Collections.emptyList())));
        for (Layout<Integer> layout : layouts) assertTrue(layout.compute(directed(0)).getPositions().isEmpty());
        Pair<Double> pin = new Pair<>(50.0, 60.0);
        LayoutRequest<Integer> request = LayoutRequest.<Integer>builder()
            .initialPositions(Collections.singletonMap(0, pin)).pinnedNodes(Collections.singleton(0)).build();
        for (Layout<Integer> layout : Arrays.<Layout<Integer>>asList(new RadialLayout<>(), new TreeLayout<>(), new BipartiteLayout<>(),
            new MultipartiteLayout<>(Arrays.asList(Collections.singletonList(0), Collections.emptyList()))))
            assertEquals(pin, layout.compute(directed(1), request).getPositions().get(0));
    }

    @Test
    public void packingSeparatesComponentsAndPreservesInternalVectors()
    {
        Graph<Integer> graph = directed(5); graph.addEdge(0, 1); graph.addEdge(2, 3);
        Map<Integer, Pair<Double>> points = new LinkedHashMap<>();
        points.put(0, new Pair<>(0.0, 0.0)); points.put(1, new Pair<>(2.0, 1.0));
        points.put(2, new Pair<>(0.0, 0.0)); points.put(3, new Pair<>(-1.0, 3.0)); points.put(4, new Pair<>(0.0, 0.0));
        LayoutRequest<Integer> request = LayoutRequest.defaults();
        ComponentPacking<Integer> packing = new ComponentPacking<>(2);
        LayoutResult<Integer> before = new LayoutResult<>(points, new LayoutDiagnostics("fixture", LayoutDiagnostics.Termination.COMPLETED));
        LayoutResult<Integer> after = packing.process(graph, before, request);
        assertEquals(points, before.getPositions());
        assertEquals(2, after.getPositions().get(1).v1() - after.getPositions().get(0).v1(), EPSILON);
        assertEquals(1, after.getPositions().get(1).v2() - after.getPositions().get(0).v2(), EPSILON);
        assertEquals(-1, after.getPositions().get(3).v1() - after.getPositions().get(2).v1(), EPSILON);
        assertEquals(3, after.getPositions().get(3).v2() - after.getPositions().get(2).v2(), EPSILON);
        List<List<Integer>> components = Arrays.asList(Arrays.asList(0, 1), Arrays.asList(2, 3), Collections.singletonList(4));
        assertBoxesSeparated(components, after.getPositions(), 2);
        assertEquals(after.getPositions(), packing.process(graph, before, request).getPositions());
        assertSame(before.getDiagnostics(), after.getDiagnostics());
    }

    private void assertBoxesSeparated(List<List<Integer>> components, Map<Integer, Pair<Double>> points, double gap)
    {
        List<Bounds2D> boxes = new ArrayList<>();
        for (List<Integer> component : components)
        {
            List<Pair<Double>> coordinates = new ArrayList<>();
            component.forEach(node -> coordinates.add(points.get(node)));
            Bounds2D box = Bounds2D.of(coordinates);
            for (Bounds2D other : boxes)
                assertTrue(box.getMinX() - other.getMaxX() >= gap - EPSILON || other.getMinX() - box.getMaxX() >= gap - EPSILON
                    || box.getMinY() - other.getMaxY() >= gap - EPSILON || other.getMinY() - box.getMaxY() >= gap - EPSILON);
            boxes.add(box);
        }
    }

    @Test
    public void packingAnchorsEntirePinnedComponents()
    {
        Graph<Integer> graph = directed(3); graph.addEdge(0, 1);
        Map<Integer, Pair<Double>> points = Map.of(0, new Pair<>(10.0, 10.0), 1, new Pair<>(20.0, 10.0), 2, new Pair<>(15.0, 10.0));
        LayoutRequest<Integer> request = LayoutRequest.<Integer>builder().initialPositions(points).pinnedNodes(Collections.singleton(0)).build();
        Layout<Integer> pipeline = new LayoutPipeline<>(new PresetLayout<>(), Collections.singletonList(new ComponentPacking<>(5)));
        LayoutResult<Integer> result = pipeline.compute(graph, request);
        assertEquals(points.get(0), result.getPositions().get(0));
        assertEquals(points.get(1), result.getPositions().get(1));
        assertTrue(result.getPositions().get(2).v1() >= 25);
    }

    @Test
    public void packingRejectsConflictingPinsAndMissingGraphContext()
    {
        Graph<Integer> graph = directed(2);
        Map<Integer, Pair<Double>> points = Map.of(0, new Pair<>(0.0, 0.0), 1, new Pair<>(0.0, 0.0));
        LayoutRequest<Integer> request = LayoutRequest.<Integer>builder().initialPositions(points).pinnedNodes(Set.of(0, 1)).build();
        LayoutResult<Integer> result = new PresetLayout<Integer>().compute(graph, request);
        ComponentPacking<Integer> packing = new ComponentPacking<>(1);
        assertThrows(IllegalArgumentException.class, () -> packing.process(graph, result, request));
        assertThrows(IllegalArgumentException.class, () -> packing.process(result, request));
        LayoutResult<Integer> incomplete = new LayoutResult<>(Collections.singletonMap(0, points.get(0)), result.getDiagnostics());
        assertThrows(IllegalArgumentException.class, () -> packing.process(graph, incomplete, request));
    }

    @Test
    public void packingLeavesEmptyAndConnectedLayoutsUnchanged()
    {
        ComponentPacking<Integer> packing = new ComponentPacking<>();
        for (Graph<Integer> graph : Arrays.asList(directed(0), tree()))
        {
            LayoutResult<Integer> result = new CircularLayout<Integer>().compute(graph);
            assertSame(result, packing.process(graph, result, LayoutRequest.defaults()));
        }
    }

    @Test
    public void graphAwarePipelineStillSupportsCoordinateOnlyProcessors()
    {
        Graph<Integer> graph = directed(3);
        Layout<Integer> pipeline = new LayoutPipeline<>(new CircularLayout<>(),
            Arrays.asList(new ComponentPacking<>(2), new CoordinateTransform<>(2, 2, 10, 10)));
        assertEquals(3, pipeline.compute(graph).getPositions().size());
    }

    @Test
    public void invalidRootsAndSpacingAreRejected()
    {
        assertThrows(IllegalArgumentException.class, () -> new RadialLayout<Integer>(9, EdgeOrientation.OUT, 1).compute(directed(1)));
        assertThrows(IllegalArgumentException.class, () -> new TreeLayout<Integer>(9, 1, 1).compute(directed(1)));
        assertThrows(IllegalArgumentException.class, () -> new RadialLayout<>(null, EdgeOrientation.OUT, 0));
        assertThrows(IllegalArgumentException.class, () -> new TreeLayout<>(null, 1, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new BipartiteLayout<>(1, 1, -1));
        assertThrows(IllegalArgumentException.class, () -> new ComponentPacking<>(Double.POSITIVE_INFINITY));
    }
}
