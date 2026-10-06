/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.gui;


import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.fast.FastDirectedUnweightedGraph;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/** Tests the JSON-compatible layout request and response contract. */
public class LayoutControllerTest
{
    private Graph<String> graph()
    {
        Graph<String> graph = new FastDirectedUnweightedGraph<>();
        graph.addEdge("a", "b");
        graph.addNode("c");
        return graph;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Double>> positions(Map<String, Object> result)
    {
        return (Map<String, Map<String, Double>>) result.get("positions");
    }

    @Test
    public void allTenAlgorithmsReturnCoordinatesWithoutChangingTopology()
    {
        Graph<String> graph = graph();
        for (String algorithm : Arrays.asList("preset", "random", "grid", "circular", "shell", "concentric", "radial", "tree", "bipartite", "multipartite"))
        {
            Map<String, Object> request = new HashMap<>();
            request.put("algorithm", algorithm);
            if ("multipartite".equals(algorithm)) request.put("partitions", List.of(List.of("a", "c"), List.of("b")));
            request.put("positions", Map.of("a", Map.of("x", 1, "y", 2), "b", Map.of("x", 3, "y", 4), "c", Map.of("x", 5, "y", 6)));
            Map<String, Object> response = LayoutController.compute(graph, request);
            assertEquals(algorithm, response.get("algorithm"));
            assertEquals("COMPLETED", response.get("termination"));
            assertEquals(Set.of("a", "b", "c"), positions(response).keySet());
            positions(response).values().forEach(point -> {
                assertTrue(Double.isFinite(point.get("x")));
                assertTrue(Double.isFinite(point.get("y")));
            });
            assertEquals(3, graph.getVertexCount());
            assertEquals(1, graph.getEdgeCount());
        }
    }

    @Test
    public void suppliedOrderingAndParametersReachTheLayout()
    {
        Map<String, Map<String, Double>> result = positions(LayoutController.compute(graph(),
            Map.of("algorithm", "grid", "nodeOrder", List.of("c", "b", "a"), "params", Map.of("columns", 1, "spacing", 10))));
        assertEquals(List.of("c", "b", "a"), new ArrayList<>(result.keySet()));
        assertEquals(-10, result.get("c").get("y"), 1e-10);
        assertEquals(10, result.get("a").get("y"), 1e-10);
    }

    @Test
    public void randomSeedIsReproducible()
    {
        Map<String, Object> request = Map.of("algorithm", "random", "params", Map.of("seed", -42, "width", 100, "height", 100));
        assertEquals(positions(LayoutController.compute(graph(), request)), positions(LayoutController.compute(graph(), request)));
    }

    @Test
    public void shellGroupsAndConcentricScoresReachTheLayout()
    {
        Map<String, Map<String, Double>> shells = positions(LayoutController.compute(graph(),
            Map.of("algorithm", "shell", "shells", List.of(List.of("c"), List.of("a", "b")), "params", Map.of("spacing", 20))));
        assertEquals(0, shells.get("c").get("x"), 1e-10);
        assertEquals(20, Math.hypot(shells.get("a").get("x"), shells.get("a").get("y")), 1e-10);
        Map<String, Map<String, Double>> concentric = positions(LayoutController.compute(graph(),
            Map.of("algorithm", "concentric", "scores", Map.of("a", 1, "b", 1, "c", 8))));
        assertEquals(0, concentric.get("c").get("x"), 1e-10);
        assertEquals(50, Math.hypot(concentric.get("a").get("x"), concentric.get("a").get("y")), 1e-10);
    }

    @Test
    public void pinnedCoordinatesAreRetained()
    {
        Map<String, Map<String, Double>> result = positions(LayoutController.compute(graph(),
            Map.of("algorithm", "circular", "positions", Map.of("a", Map.of("x", 7, "y", 9)), "pinnedNodes", List.of("a"))));
        assertEquals(7, result.get("a").get("x"), 0);
        assertEquals(9, result.get("a").get("y"), 0);
    }

    @Test
    public void invalidAlgorithmsAndParametersFail()
    {
        assertThrows(IllegalArgumentException.class, () -> LayoutController.compute(graph(), Map.of("algorithm", "invalid")));
        assertThrows(IllegalArgumentException.class, () -> LayoutController.compute(graph(), Map.of("algorithm", "grid", "params", Map.of("columns", 1.5))));
        assertThrows(IllegalArgumentException.class, () -> LayoutController.compute(graph(), Map.of("algorithm", "grid", "params", Map.of("columns", -1))));
        assertThrows(IllegalArgumentException.class, () -> LayoutController.compute(graph(), Map.of("algorithm", "random", "params", Map.of("seed", "9223372036854775808"))));
        assertThrows(IllegalArgumentException.class, () -> LayoutController.compute(graph(), Map.of("algorithm", "circular", "params", Map.of("radius", Double.NaN))));
        assertThrows(IllegalArgumentException.class, () -> LayoutController.compute(graph(), Map.of("algorithm", "preset")));
    }

    @Test
    public void malformedDataAndNodeCoverageFail()
    {
        assertThrows(IllegalArgumentException.class, () -> LayoutController.compute(graph(), Map.of("algorithm", "shell", "shells", "invalid")));
        assertThrows(IllegalArgumentException.class, () -> LayoutController.compute(graph(), Map.of("algorithm", "shell", "shells", List.of(List.of("a")))));
        assertThrows(IllegalArgumentException.class, () -> LayoutController.compute(graph(), Map.of("algorithm", "concentric", "scores", Map.of("a", 2))));
        assertThrows(IllegalArgumentException.class, () -> LayoutController.compute(graph(), Map.of("algorithm", "concentric", "scores", Map.of("a", 2, "b", 1, "c", Double.POSITIVE_INFINITY))));
        assertThrows(IllegalArgumentException.class, () -> LayoutController.compute(graph(), Map.of("algorithm", "grid", "nodeOrder", List.of(1, 2, 3))));
        assertThrows(IllegalArgumentException.class, () -> LayoutController.compute(graph(), Map.of("algorithm", "preset", "positions", Map.of("a", Map.of("x", "bad", "y", 2)))));
    }

    @Test
    public void radialAndTreeUseRootsAndDirection()
    {
        Map<String, Map<String, Double>> radial = positions(LayoutController.compute(graph(),
            Map.of("algorithm", "radial", "params", Map.of("root", "b", "direction", "IN", "spacing", 10))));
        assertEquals(0, radial.get("b").get("x"), 0);
        assertEquals(10, Math.hypot(radial.get("a").get("x"), radial.get("a").get("y")), 1e-10);
        Map<String, Map<String, Double>> tree = positions(LayoutController.compute(graph(),
            Map.of("algorithm", "tree", "params", Map.of("root", "a", "levelSpacing", 20))));
        assertEquals(20, tree.get("b").get("y"), 1e-10);
        assertThrows(IllegalArgumentException.class, () -> LayoutController.compute(graph(), Map.of("algorithm", "radial", "params", Map.of("root", "missing"))));
        assertThrows(IllegalArgumentException.class, () -> LayoutController.compute(graph(), Map.of("algorithm", "tree", "params", Map.of("root", "b"))));
    }

    @Test
    public void partitionRequestsValidateSidesAndTopology()
    {
        assertThrows(IllegalArgumentException.class, () -> LayoutController.compute(graph(), Map.of("algorithm", "multipartite")));
        assertThrows(IllegalArgumentException.class, () -> LayoutController.compute(graph(), Map.of("algorithm", "bipartite", "partitions", List.of(List.of("a", "b", "c")))));
        assertThrows(IllegalArgumentException.class, () -> LayoutController.compute(graph(), Map.of("algorithm", "multipartite", "partitions", List.of(List.of("a", "b"), List.of("c")))));
        assertThrows(IllegalArgumentException.class, () -> LayoutController.compute(graph(), Map.of("algorithm", "bipartite", "params", Map.of("sweeps", 101))));
    }

    @Test
    public void optionalPackingChangesOnlyComponentPlacement()
    {
        Map<String, Object> request = Map.of("algorithm", "preset", "positions",
            Map.of("a", Map.of("x", 0, "y", 0), "b", Map.of("x", 2, "y", 1), "c", Map.of("x", 0, "y", 0)),
            "params", Map.of("packComponents", true, "packingGap", 10));
        Map<String, Map<String, Double>> result = positions(LayoutController.compute(graph(), request));
        assertEquals(2, result.get("b").get("x") - result.get("a").get("x"), 1e-10);
        assertEquals(1, result.get("b").get("y") - result.get("a").get("y"), 1e-10);
        assertNotEquals(result.get("a"), result.get("c"));
        assertThrows(IllegalArgumentException.class, () -> LayoutController.compute(graph(), Map.of("algorithm", "grid", "params", Map.of("packComponents", "true"))));
    }
}
