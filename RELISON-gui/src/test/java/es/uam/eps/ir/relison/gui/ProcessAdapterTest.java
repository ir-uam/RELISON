package es.uam.eps.ir.relison.gui;

import es.uam.eps.ir.relison.graph.fast.FastDirectedWeightedGraph;
import es.uam.eps.ir.relison.grid.sna.vertex.VertexMetricIdentifiers;
import es.uam.eps.ir.relison.grid.sna.pair.PairMetricIdentifiers;
import es.uam.eps.ir.relison.grid.diffusion.protocol.ProtocolIdentifiers;
import es.uam.eps.ir.relison.grid.diffusion.stop.StopConditionIdentifiers;
import io.javalin.http.*;
import org.junit.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

/** Exercises the actual request adapters and controllers without requiring sockets. */
public class ProcessAdapterTest
{
    private static class Request {
        final String id = UUID.randomUUID().toString();
        final Map<String, Object> body;
        volatile int status = 200;
        volatile Object response;
        final Context ctx;
        Request(Map<String, Object> body) {
            this.body = body;
            ctx = (Context) Proxy.newProxyInstance(Context.class.getClassLoader(), new Class<?>[]{Context.class}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "bodyAsClass": return body;
                    case "header": return args.length == 1 ? id : proxy;
                    case "status": if (args == null || args.length == 0) return HttpStatus.forStatus(status); status = (Integer) args[0]; return proxy;
                    case "json": response = args[0]; return proxy;
                    case "result": return response == null ? "" : response.toString();
                    case "pathParamMap": return Map.of();
                    case "toString": return "Test request";
                    default: throw new UnsupportedOperationException(method.getName());
                }
            });
        }
    }
    private GraphSession graph(GraphStore store) {
        FastDirectedWeightedGraph<String> graph = new FastDirectedWeightedGraph<>();
        graph.addEdge("a", "b", 2); graph.addEdge("b", "c", 3); graph.addEdge("c", "a", 4);
        GraphSession session = new GraphSession("graph", graph, true, true, false, false); store.register(session); return session;
    }
    private Map<String, Object> row(JobManager manager, Request request) {
        return manager.snapshots().stream().filter(job -> request.id.equals(job.get("id"))).findFirst().orElseThrow();
    }

    @Test public void metricRequestsExposeResultsMetadataAndAggregatePairsInOnePass() throws Exception {
        try (JobManager manager = new JobManager()) {
            GraphStore store = new GraphStore(); graph(store);
            ProcessController adapter = new ProcessController(manager); MetricController controller = new MetricController(store, manager);
            Request node = new Request(Map.of("graphId", "graph", "metric", VertexMetricIdentifiers.DEGREE));
            adapter.track("vertex", controller::vertex).handle(node.ctx);
            assertEquals(200, node.status); assertEquals("completed", row(manager, node).get("state"));
            assertEquals(3L, row(manager, node).get("completed"));
            assertEquals(3, ((Map<?, ?>) ((Map<?, ?>) node.response).get("values")).size());
            Request pairs = new Request(Map.of("graphId", "graph", "metric", PairMetricIdentifiers.WEIGHT, "onlyLinks", false));
            adapter.track("pair", controller::pair).handle(pairs.ctx);
            assertEquals(200, pairs.status); assertEquals(6L, ((Map<?, ?>) pairs.response).get("evaluated"));
            assertEquals("Computing pairs: statistics", row(manager, pairs).get("phase"));
            assertEquals(6L, row(manager, pairs).get("completed")); assertEquals(6L, row(manager, pairs).get("total"));
            List<?> valueCounts = (List<?>) ((Map<?, ?>) pairs.response).get("valueCounts");
            assertEquals(6L, valueCounts.stream().mapToLong(value -> ((Number) ((Map<?, ?>) value).get("count")).longValue()).sum());
        }
    }
    @Test public void processMetadataPreservesRecommendationModeForTypeBadges() throws Exception {
        try (JobManager manager = new JobManager()) {
            ProcessController adapter = new ProcessController(manager);
            Request request = new Request(Map.of("graphId", "graph", "mode", "prediction", "algorithm", "Common Neighbours"));
            adapter.track("recommendation", ctx -> ctx.json(Map.of("ok", true))).handle(request.ctx);
            Map<?, ?> parameters = (Map<?, ?>) row(manager, request).get("parameters");
            assertEquals("prediction", parameters.get("mode"));
            assertEquals("Common Neighbours", parameters.get("algorithm"));
        }
    }
    @Test public void selectingComputedRecommendationsUpdatesAugmentedGraphWithoutRecomputing() throws Exception {
        try (JobManager manager = new JobManager()) {
            GraphStore store = new GraphStore(); GraphSession session = graph(store);
            RecommendationResult rec = new RecommendationResult("Model", "recommendation", 10,
                    List.of(new RecommendationResult.RecEdge("a", "c", 2)));
            RecommendationResult prediction = new RecommendationResult("Model", "prediction", 5,
                    List.of(new RecommendationResult.RecEdge("b", "a", 3)));
            session.getRecommendations().put("rec", rec); session.getRecommendations().put("prediction", prediction);
            session.setActiveRecommendation("rec");
            es.uam.eps.ir.relison.graph.Graph<String> previous = session.getAugmentedGraph();
            assertTrue(previous.containsEdge("a", "c"));
            ProcessController adapter = new ProcessController(manager);
            RecommendationController controller = new RecommendationController(store, manager);
            Request select = new Request(Map.of("graphId", "graph", "key", "prediction"));
            adapter.mutation(controller::select).handle(select.ctx);
            assertEquals(200, select.status); assertEquals(Map.of("key", "prediction"), select.response);
            assertSame(prediction, session.getActiveRecommendation());
            assertTrue(session.getAugmentedGraph().containsEdge("b", "a"));
            assertFalse(session.getAugmentedGraph().containsEdge("a", "c"));
            assertNotSame(previous, session.getAugmentedGraph());
            Request selectRec = new Request(Map.of("graphId", "graph", "key", "rec"));
            adapter.mutation(controller::select).handle(selectRec.ctx);
            assertSame(rec, session.getActiveRecommendation());
            assertEquals(2, session.getRecommendations().size()); assertTrue(manager.snapshots().isEmpty());
            assertFalse(session.getGraph().containsEdge("a", "c"));
            assertFalse(session.getGraph().containsEdge("b", "a"));
        }
    }
    @Test public void selectingUnknownResultsPreservesTheActiveRecommendation() throws Exception {
        try (JobManager manager = new JobManager()) {
            GraphStore store = new GraphStore(); GraphSession session = graph(store);
            session.getRecommendations().put("rec", new RecommendationResult("Model", "recommendation", 10, List.of()));
            session.setActiveRecommendation("rec");
            RecommendationController controller = new RecommendationController(store, manager);
            ProcessController adapter = new ProcessController(manager);
            Request missing = new Request(Map.of("graphId", "graph", "key", "missing"));
            adapter.mutation(controller::select).handle(missing.ctx);
            assertEquals(404, missing.status); assertEquals("rec", session.getActiveRecommendationKey());
            Request unknownGraph = new Request(Map.of("graphId", "unknown", "key", "rec"));
            adapter.mutation(controller::select).handle(unknownGraph.ctx);
            assertEquals(404, unknownGraph.status); assertEquals("rec", session.getActiveRecommendationKey());
        }
    }
    @Test public void cancellationAndMutationResponsesReflectTheActualWorkerState() throws Exception {
        try (JobManager manager = new JobManager()) {
            ProcessController adapter = new ProcessController(manager);
            CountDownLatch started = new CountDownLatch(1), cleanup = new CountDownLatch(1);
            Request request = new Request(Map.of("graphId", "graph"));
            CompletableFuture<Void> running = CompletableFuture.runAsync(() -> {
                try {
                    adapter.track("community", ctx -> {
                        started.countDown();
                        try { new CountDownLatch(1).await(); } finally { cleanup.countDown(); }
                    }).handle(request.ctx);
                } catch (Exception error) { throw new CompletionException(error); }
            });
            assertTrue(started.await(5, TimeUnit.SECONDS));
            Request edit = new Request(Map.of("graphId", "graph"));
            adapter.mutation(ctx -> fail("Must not mutate")).handle(edit.ctx); assertEquals(409, edit.status);
            assertTrue(manager.cancel(request.id, "graph")); running.get(5, TimeUnit.SECONDS);
            assertEquals(0, cleanup.getCount()); assertEquals(200, request.status);
            assertEquals(true, ((Map<?, ?>) request.response).get("cancelled"));
            assertEquals("cancelled", row(manager, request).get("state"));
            Request invalid = new Request(Map.of("graphId", "graph"));
            adapter.track("vertex", ctx -> ctx.status(400).json(Map.of("error", "Bad metric"))).handle(invalid.ctx);
            assertEquals("failed", row(manager, invalid).get("state"));
            assertTrue(row(manager, invalid).get("error").toString().contains("Bad metric"));
        }
    }
    @Test public void diffusionRequestPublishesAReadableFileAndReportsTurns() throws Exception {
        try (JobManager manager = new JobManager()) {
            GraphStore store = new GraphStore(); GraphSession session = graph(store);
            ProcessController adapter = new ProcessController(manager); DiffusionController controller = new DiffusionController(store, manager);
            Request request = new Request(Map.of("graphId", "graph", "protocol", Map.of("id", ProtocolIdentifiers.SIMPLE),
                    "stop", Map.of("id", StopConditionIdentifiers.NUMITER, "params", Map.of("numIter", 2))));
            try {
                adapter.track("diffusion", controller::run).handle(request.ctx);
                assertEquals(String.valueOf(request.response), 200, request.status);
                assertNotNull(session.getDiffusion()); assertEquals(3, session.getDiffusion().numIterations);
                int[] read = {0}; session.getDiffusion().eachIteration(3, (index, iteration) -> read[0]++);
                assertEquals(3, read[0]); assertEquals(3L, row(manager, request).get("completed"));
                assertEquals("turns", row(manager, request).get("unit"));
            } finally { session.setDiffusion(null); }
        }
    }

    @Test public void cancellingDiffusionClosesAndDeletesItsPartialFileAndKeepsThePreviousResult() throws Exception {
        java.nio.file.Path directory = java.nio.file.Files.createTempDirectory("relison-cancellation-test-");
        String previousTemp = System.getProperty("java.io.tmpdir");
        try (JobManager manager = new JobManager()) {
            System.setProperty("java.io.tmpdir", directory.toString());
            GraphStore store = new GraphStore(); GraphSession session = graph(store);
            ProcessController adapter = new ProcessController(manager); DiffusionController controller = new DiffusionController(store, manager);
            Request completed = new Request(Map.of("graphId", "graph", "protocol", Map.of("id", ProtocolIdentifiers.SIMPLE),
                    "stop", Map.of("id", StopConditionIdentifiers.NUMITER, "params", Map.of("numIter", 1))));
            adapter.track("diffusion", controller::run).handle(completed.ctx);
            DiffusionResult previous = session.getDiffusion(); assertNotNull(previous);
            Request cancelled = new Request(Map.of("graphId", "graph", "protocol", Map.of("id", ProtocolIdentifiers.SIMPLE),
                    "stop", Map.of("id", StopConditionIdentifiers.NUMITER, "params", Map.of("numIter", 1_000_000))));
            CompletableFuture<Void> running = CompletableFuture.runAsync(() -> {
                try { adapter.track("diffusion", controller::run).handle(cancelled.ctx); }
                catch (Exception error) { throw new CompletionException(error); }
            });
            try {
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                boolean progressed = false;
                while (System.nanoTime() < deadline && !progressed) {
                    progressed = manager.snapshots().stream().anyMatch(job -> cancelled.id.equals(job.get("id"))
                            && "turns".equals(job.get("unit")) && ((Number) job.get("completed")).longValue() > 0);
                    if (!progressed) Thread.sleep(1);
                }
                assertTrue("Diffusion should reach a completed turn", progressed);
                assertTrue(manager.cancel(cancelled.id, "graph")); running.get(5, TimeUnit.SECONDS);
                assertEquals(true, ((Map<?, ?>) cancelled.response).get("cancelled"));
                assertSame(previous, session.getDiffusion());
                try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.list(directory)) { assertEquals(1, files.count()); }
                assertTrue(java.nio.file.Files.exists(java.nio.file.Path.of(previous.simFile)));
            } finally { manager.cancel(cancelled.id, "graph"); running.get(5, TimeUnit.SECONDS); session.setDiffusion(null); }
        } finally {
            System.setProperty("java.io.tmpdir", previousTemp);
            java.nio.file.Files.delete(directory);
        }
    }
}
