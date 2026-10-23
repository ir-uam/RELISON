package es.uam.eps.ir.relison.gui;

import es.uam.eps.ir.relison.graph.fast.FastDirectedWeightedGraph;
import es.uam.eps.ir.relison.sna.community.Communities;
import org.junit.Test;
import es.uam.eps.ir.relison.grid.sna.vertex.VertexMetricIdentifiers;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.Assert.*;

public class MetricRecommendationTest
{
    private static class Request
    {
        int status = 200; Object response; final Context ctx;
        Request(Map<String, Object> body) {
            String id = UUID.randomUUID().toString();
            ctx = (Context) Proxy.newProxyInstance(Context.class.getClassLoader(), new Class<?>[]{Context.class}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "bodyAsClass": return body;
                    case "header": return args.length == 1 ? id : proxy;
                    case "status": if (args == null || args.length == 0) return HttpStatus.forStatus(status); status = (Integer) args[0]; return proxy;
                    case "json": response = args[0]; return proxy;
                    case "result": return response == null ? "" : response.toString();
                    case "pathParamMap": return Map.of();
                    default: throw new UnsupportedOperationException(method.getName());
                }
            });
        }
    }
    private GraphSession session()
    {
        FastDirectedWeightedGraph<String> graph = new FastDirectedWeightedGraph<>();
        graph.addEdge("a", "b", 2.0); graph.addNode("c");
        GraphSession session = new GraphSession("test", graph, true, true, false, false);
        session.getRecommendations().put("A", new RecommendationResult("A", "recommendation", 1,
                List.of(new RecommendationResult.RecEdge("b", "c", 1.0))));
        session.getRecommendations().put("B", new RecommendationResult("B", "recommendation", 1,
                List.of(new RecommendationResult.RecEdge("c", "a", 1.0))));
        session.setActiveRecommendation("A");
        return session;
    }

    @Test public void explicitRecommendationDoesNotSwitchOverlayOrMutateBaseGraph()
    {
        GraphSession session = session(), view = session.forMetricRecommendation("B");
        assertEquals("A", session.getActiveRecommendationKey());
        assertEquals("B", view.getActiveRecommendationKey());
        assertTrue(view.getAugmentedGraph().containsEdge("c", "a"));
        assertFalse(view.getAugmentedGraph().containsEdge("b", "c"));
        assertTrue(session.getAugmentedGraph().containsEdge("b", "c"));
        assertFalse(session.getAugmentedGraph().containsEdge("c", "a"));
        assertEquals(1L, session.getGraph().getEdgeCount());
        assertEquals(2.0, view.getAugmentedGraph().getEdgeWeight("a", "b"), 0.0);
        assertTrue(MetricController.useRecommendation(Map.of("recommendationKey", "B"), view));
    }

    @Test public void repeatedMetricsReuseTheirGraphAndDistancesAndKeepPartitionContext()
    {
        GraphSession session = session();
        Communities<String> communities = new Communities<>(); communities.addCommunity(); communities.add("a", 0);
        session.getCommunities().put("partition", communities);
        GraphSession first = session.forMetricRecommendation("B"), second = session.forMetricRecommendation("B");
        assertSame(first, second);
        assertSame(first.getAugmentedGraph(), second.getAugmentedGraph());
        assertSame(first.getAugmentedDistanceCalculator(), second.getAugmentedDistanceCalculator());
        assertSame(communities, second.getCommunities().get("partition"));
        assertSame(session, session.forMetricRecommendation("A"));
    }

    @Test public void changingOrRemovingARecommendationCannotReuseAnObsoleteComputationView()
    {
        GraphSession session = session(), old = session.forMetricRecommendation("B");
        session.getRecommendations().put("B", new RecommendationResult("new B", "prediction", 1,
                List.of(new RecommendationResult.RecEdge("a", "c", 1.0))));
        GraphSession replacement = session.forMetricRecommendation("B");
        assertNotSame(old, replacement); assertTrue(replacement.getAugmentedGraph().containsEdge("a", "c"));
        assertFalse(replacement.getAugmentedGraph().containsEdge("c", "a"));
        session.invalidateCaches();
        try { session.forMetricRecommendation("B"); fail("Removed recommendations must fail explicitly"); }
        catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("Unknown recommendation")); }
    }

    @Test public void actualMetricAdapterComputesTheRequestedRecommendationAndReportsItsIdentity() throws Exception
    {
        GraphSession session = session(); GraphStore store = new GraphStore(); store.register(session);
        try (JobManager jobs = new JobManager()) {
            MetricController controller = new MetricController(store, jobs);
            Request request = new Request(Map.of("graphId", "test", "metric", VertexMetricIdentifiers.DEGREE,
                    "params", Map.of("orientation", "OUT"), "withRecommendation", true, "recommendationKey", "B"));
            new ProcessController(jobs).track("vertex", controller::vertex).handle(request.ctx);
            assertEquals(200, request.status);
            Map<?, ?> response = (Map<?, ?>) request.response;
            assertEquals("B", response.get("recommendation"));
            Map<?, ?> values = (Map<?, ?>) response.get("values");
            assertEquals(1.0, ((Number) values.get("c")).doubleValue(), 0.0);
            assertEquals(0.0, ((Number) values.get("b")).doubleValue(), 0.0);
            assertEquals("A", session.getActiveRecommendationKey());
        }
    }

    @Test public void unknownRecommendationFailsInsteadOfQuietlyComputingTheBaseGraph()
    {
        GraphSession session = session(); GraphStore store = new GraphStore(); store.register(session);
        try (JobManager jobs = new JobManager()) {
            Request request = new Request(Map.of("graphId", "test", "metric", VertexMetricIdentifiers.DEGREE, "recommendationKey", "missing"));
            new MetricController(store, jobs).vertex(request.ctx);
            assertEquals(400, request.status); assertTrue(((Map<?, ?>) request.response).get("error").toString().contains("Unknown recommendation"));
        }
    }
}
