package es.uam.eps.ir.relison.gui;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.fast.FastDirectedWeightedGraph;
import es.uam.eps.ir.relison.graph.fast.FastUndirectedWeightedGraph;
import es.uam.eps.ir.relison.graph.multigraph.fast.FastDirectedWeightedMultiGraph;
import es.uam.eps.ir.relison.sna.community.Communities;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/** Community exploration uses RELISON's generators without modifying the source graph or partition. */
public class CommunityGraphTest
{
    private Communities<String> partition()
    {
        Communities<String> communities = new Communities<>();
        for (int i = 0; i < 3; i++) communities.addCommunity();
        communities.add("a", 0); communities.add("b", 0); communities.add("c", 1); communities.add("isolate", 2);
        return communities;
    }

    private Graph<String> graph(boolean directed)
    {
        Graph<String> graph = directed ? new FastDirectedWeightedGraph<>() : new FastUndirectedWeightedGraph<>();
        graph.addEdge("a", "b", 2.0); graph.addEdge("a", "c", 4.0); graph.addEdge("b", "c", 3.0);
        graph.addEdge("b", "b", 7.0); graph.addNode("isolate");
        if (directed) graph.addEdge("c", "a", 5.0);
        return graph;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> links(Map<String, Object> result)
    {
        Map<String, Double> links = new LinkedHashMap<>();
        for (Map<String, Object> edge : (List<Map<String, Object>>) result.get("edges"))
            links.put(edge.get("source") + ">" + edge.get("target"), ((Number) edge.get("weight")).doubleValue());
        return links;
    }

    @Test
    public void weightedGraphPreservesDirectionInternalLinksAndIsolates()
    {
        Graph<String> graph = graph(true);
        Map<String, Object> result = CommunityController.communityGraph(graph, partition(), Map.of());
        assertEquals(true, result.get("directed"));
        assertEquals(Map.of("0>0", 9.0, "0>1", 7.0, "1>0", 5.0), links(result));
        assertEquals(3, ((List<?>) result.get("nodes")).size());
        assertEquals(3, ((Map<?, ?>) result.get("positions")).size());
        assertEquals(4, graph.getVertexCount()); assertEquals(5, graph.getEdgeCount());
    }

    @Test
    public void undirectedWeightsAndSelfLoopsAreCountedOnce()
    {
        Map<String, Object> result = CommunityController.communityGraph(graph(false), partition(), Map.of());
        assertEquals(false, result.get("directed"));
        assertEquals(Map.of("0>0", 9.0, "0>1", 7.0), links(result));
    }

    @Test
    public void multigraphWeightsIncludeEveryParallelEdgeWhileCountModesRetainGeneratorSemantics()
    {
        Graph<String> graph = new FastDirectedWeightedMultiGraph<>();
        graph.addEdge("a", "c", 2.0); graph.addEdge("a", "c", 5.0); graph.addEdge("b", "c", 3.0);
        graph.addEdge("a", "b", 11.0); graph.addNode("isolate");
        assertEquals(Map.of("0>1", 10.0, "0>0", 11.0), links(CommunityController.communityGraph(graph, partition(), Map.of())));
        // Complete/Inter generators traverse adjacent node pairs; they do not preserve source parallel multiplicity.
        assertEquals(Map.of("0>1", 2.0, "0>0", 1.0), links(CommunityController.communityGraph(graph, partition(), Map.of("mode", "complete"))));
        assertEquals(Map.of("0>1", 2.0), links(CommunityController.communityGraph(graph, partition(), Map.of("mode", "inter"))));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void planarAndSpatialLayoutsReturnFiniteCoordinatesForEveryCommunity()
    {
        for (String layout : CommunityController.GRAPH_LAYOUTS)
        {
            Map<String, Object> result = CommunityController.communityGraph(graph(true), partition(), Map.of("layout", layout));
            Map<String, Map<String, Double>> positions = (Map<String, Map<String, Double>>) result.get("positions");
            assertEquals(Set.of("0", "1", "2"), positions.keySet());
            positions.values().forEach(point -> {
                assertTrue(Double.isFinite(point.get("x"))); assertTrue(Double.isFinite(point.get("y")));
                if (layout.endsWith("-3d")) assertTrue(Double.isFinite(point.get("z")));
            });
        }
    }

    @Test
    public void uncoveredNodesAndInvalidRequestsFailClearly()
    {
        Graph<String> graph = graph(true); graph.addNode("new");
        assertThrows(IllegalArgumentException.class, () -> CommunityController.communityGraph(graph, partition(), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> CommunityController.communityGraph(graph(true), partition(), Map.of("mode", "missing")));
        assertThrows(IllegalArgumentException.class, () -> CommunityController.communityGraph(graph(true), partition(), Map.of("layout", "missing")));
    }
}
