/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.gui;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.fast.FastDirectedUnweightedGraph;
import es.uam.eps.ir.relison.graph.fast.FastUndirectedUnweightedGraph;
import es.uam.eps.ir.relison.graph.multigraph.fast.FastDirectedUnweightedMultiGraph;
import org.junit.Test;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

public class PathControllerTest
{
    private Graph<String> graph()
    {
        Graph<String> graph = new FastDirectedUnweightedGraph<>();
        graph.addEdge("a", "d");
        graph.addEdge("a", "b"); graph.addEdge("b", "c"); graph.addEdge("c", "d");
        graph.addNode("isolated");
        return graph;
    }

    @Test
    public void filteredSearchFindsLongerShortestPathWithoutMutatingGraph()
    {
        Graph<String> graph = graph();
        Map<String, Object> result = PathController.computePaths(graph, "a", "d", Map.of(
            "allowedNodes", List.of("a", "b", "c", "d"),
            "allowedLinks", List.of(List.of("a", "b"), List.of("b", "c"), List.of("c", "d"))));
        assertEquals(3, result.get("length"));
        assertEquals(List.of(List.of("a", "b", "c", "d")), result.get("paths"));
        assertEquals(4, graph.getEdgeCount());
        assertEquals(1, PathController.computePaths(graph, "a", "d", Map.of()).get("length"));
    }

    @Test
    public void exclusionsApplyDuringTraversalAndDirectionIsPreserved()
    {
        Graph<String> graph = graph();
        assertEquals(-1, PathController.computePaths(graph, "d", "a", Map.of()).get("length"));
        assertEquals(-1, PathController.computePaths(graph, "a", "d", Map.of("allowedLinks", List.of())).get("length"));
        assertEquals(-1, PathController.computePaths(graph, "a", "d", Map.of("allowedNodes", List.of("a", "c", "d"),
            "allowedLinks", List.of(List.of("a", "b"), List.of("b", "c"), List.of("c", "d")))).get("length"));
        assertEquals(0, PathController.computePaths(graph, "isolated", "isolated", Map.of()).get("length"));
    }

    @Test
    public void undirectedLinksPermitReverseTraversal()
    {
        Graph<String> graph = new FastUndirectedUnweightedGraph<>(); graph.addEdge("a", "b");
        assertEquals(List.of(List.of("b", "a")), PathController.computePaths(graph, "b", "a",
            Map.of("allowedLinks", List.of(List.of("a", "b")))).get("paths"));
    }

    @Test
    public void parallelEdgesReturnOneNodeSequenceAndArbitraryIdsAreUnambiguous()
    {
        Graph<String> graph = new FastDirectedUnweightedMultiGraph<>();
        graph.addEdge("a|b", "c"); graph.addEdge("a|b", "c"); graph.addEdge("a", "b|c");
        assertEquals(1, PathController.computePaths(graph, "a|b", "c",
            Map.of("allowedLinks", List.of(List.of("a|b", "c")))).get("count"));
        assertEquals(-1, PathController.computePaths(graph, "a", "b|c",
            Map.of("allowedLinks", List.of(List.of("a|b", "c")))).get("length"));
    }

    @Test
    public void invalidScopesFailWithUsefulErrors()
    {
        Graph<String> graph = graph();
        for (Map<String, Object> body : List.of(Map.<String, Object>of("allowedNodes", "a"), Map.<String, Object>of("allowedNodes", List.of("a")),
            Map.<String, Object>of("allowedLinks", List.of(List.of("d", "a"))), Map.<String, Object>of("allowedNodes", List.of("missing"))))
        {
            try { PathController.computePaths(graph, "a", "d", body); fail("Expected invalid scope to fail"); }
            catch (IllegalArgumentException expected) { assertFalse(expected.getMessage().isEmpty()); }
        }
    }
}
