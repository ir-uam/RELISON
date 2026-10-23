package es.uam.eps.ir.relison.viz;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.fast.FastDirectedUnweightedGraph;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
import es.uam.eps.ir.relison.viz.layouts.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Column grouping and directed hop geometry for the grid variants. */
public class GridVariantsTest
{
    private Graph<Integer> graph(int size)
    {
        Graph<Integer> graph = new FastDirectedUnweightedGraph<>();
        for (int i = 0; i < size; i++) graph.addNode(i);
        return graph;
    }

    @Test
    public void featureColumnsAllowInternalEdgesAndPreserveRequestedOrder()
    {
        Graph<Integer> graph = graph(3);
        graph.addEdge(0, 1); graph.addEdge(0, 0);
        LayoutRequest<Integer> request = LayoutRequest.<Integer>builder().nodeOrder(Arrays.asList(1, 0, 2)).build();
        Map<Integer, Pair<Double>> positions = new FeatureGridLayout<Integer>(
            Arrays.asList(Arrays.asList(0, 1), Collections.singletonList(2)), 10, 2, 0).compute(graph, request).getPositions();
        assertEquals(new Pair<>(-5.0, -1.0), positions.get(1));
        assertEquals(new Pair<>(-5.0, 1.0), positions.get(0));
        assertEquals(new Pair<>(5.0, 0.0), positions.get(2));
        assertEquals(3, new FeatureGridLayout<Integer>(Collections.singletonList(Arrays.asList(0, 1, 2))).compute(graph).getPositions().size());
    }

    @Test
    public void featureGroupsMustCoverNodesExactly()
    {
        assertThrows(IllegalArgumentException.class, () -> new FeatureGridLayout<Integer>(
            Collections.singletonList(Arrays.asList(0, 0))));
        assertThrows(IllegalArgumentException.class, () -> new FeatureGridLayout<Integer>(
            Collections.singletonList(Collections.singletonList(0))).compute(graph(2)));
        assertTrue(new FeatureGridLayout<Integer>(Collections.emptyList()).compute(graph(0)).getPositions().isEmpty());
    }

    @Test
    public void egoColumnsFollowTraversalAndSeparateUnreachableNodes()
    {
        Graph<Integer> graph = graph(5);
        graph.addEdge(0, 1); graph.addEdge(0, 2); graph.addEdge(2, 3);
        Map<Integer, Pair<Double>> positions = new EgoGridLayout<Integer>(0, EdgeOrientation.OUT, 10, 2).compute(graph).getPositions();
        assertEquals(new Pair<>(0.0, 0.0), positions.get(0));
        assertEquals(new Pair<>(10.0, -1.0), positions.get(1));
        assertEquals(new Pair<>(10.0, 1.0), positions.get(2));
        assertEquals(20.0, positions.get(3).v1(), 0);
        assertEquals(30.0, positions.get(4).v1(), 0);
        positions = new EgoGridLayout<Integer>(3, EdgeOrientation.IN, 10, 2).compute(graph).getPositions();
        assertEquals(10.0, positions.get(2).v1(), 0);
        assertEquals(20.0, positions.get(0).v1(), 0);
        assertEquals(30.0, positions.get(1).v1(), 0);
        positions = new EgoGridLayout<Integer>(0, EdgeOrientation.MUTUAL, 10, 2).compute(graph).getPositions();
        assertEquals(10.0, positions.get(3).v1(), 0);
    }

    @Test
    public void egoSupportsEmptyGraphsPinsAndInvalidRoots()
    {
        assertTrue(new EgoGridLayout<Integer>().compute(graph(0)).getPositions().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new EgoGridLayout<Integer>(9, EdgeOrientation.UND, 1, 1).compute(graph(1)));
        Pair<Double> pin = new Pair<>(12.0, 15.0);
        LayoutRequest<Integer> request = LayoutRequest.<Integer>builder().initialPositions(Collections.singletonMap(0, pin))
            .pinnedNodes(Collections.singleton(0)).build();
        assertEquals(pin, new EgoGridLayout<Integer>().compute(graph(1), request).getPositions().get(0));
    }
}
