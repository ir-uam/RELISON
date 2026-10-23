package es.uam.eps.ir.relison.gui;

import es.uam.eps.ir.relison.graph.fast.FastDirectedWeightedGraph;
import es.uam.eps.ir.relison.grid.diffusion.protocol.ProtocolIdentifiers;
import es.uam.eps.ir.relison.grid.diffusion.stop.StopConditionIdentifiers;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.Assert.*;

public class DiffusionAnalysisTest
{
    private static final class Request {
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
                    default: throw new UnsupportedOperationException(method.getName());
                }
            });
        }
        Map<?, ?> json() { return (Map<?, ?>) response; }
    }
    private static GraphSession graph(GraphStore store) {
        FastDirectedWeightedGraph<String> graph = new FastDirectedWeightedGraph<>();
        graph.addEdge("a", "b", 1.0); graph.addEdge("b", "c", 1.0); graph.addEdge("c", "a", 1.0);
        GraphSession session = new GraphSession("g", graph, true, true, false, false); store.register(session); return session;
    }
    private static Request simulate(DiffusionController controller, ProcessController adapter, int turns) throws Exception {
        Request request = new Request(Map.of("graphId", "g", "protocol", Map.of("id", ProtocolIdentifiers.SIMPLE),
                "stop", Map.of("id", StopConditionIdentifiers.NUMITER, "params", Map.of("numIter", turns))));
        adapter.track("diffusion", controller::run).handle(request.ctx);
        assertEquals(String.valueOf(request.response), 200, request.status); return request;
    }

    @Test public void retainedRunsAreQueriedByIdAndTheirFilesHaveIndependentLifetimes() throws Exception {
        try (JobManager jobs = new JobManager()) {
            GraphStore store = new GraphStore(); GraphSession session = graph(store);
            DiffusionController controller = new DiffusionController(store, jobs); ProcessController adapter = new ProcessController(jobs);
            try {
                Request first = simulate(controller, adapter, 1); DiffusionResult a = session.getDiffusion();
                Request second = simulate(controller, adapter, 3); DiffusionResult b = session.getDiffusion();
                assertNotEquals(first.json().get("runId"), second.json().get("runId"));
                assertSame(a, session.getDiffusion(a.id)); assertSame(b, session.getDiffusion(b.id));
                Request playback = new Request(Map.of("graphId", "g", "runId", a.id)); controller.result(playback.ctx);
                assertEquals(a.id, playback.json().get("runId")); assertEquals(a.numIterations, playback.json().get("numIterations"));
                assertEquals(a.numIterations, ((List<?>)playback.json().get("iterations")).size());
                assertSame(b, session.getDiffusion());
                assertTrue(Files.exists(Path.of(a.simFile))); assertTrue(Files.exists(Path.of(b.simFile)));
                Request trajectory = new Request(Map.of("graphId", "g", "runId", a.id, "node", "a", "pieces", true));
                adapter.track("diffusionAnalysis", controller::trajectory).handle(trajectory.ctx);
                assertEquals(a.numIterations, trajectory.json().get("iterations"));
                assertTrue(jobs.snapshots().stream().anyMatch(row -> "diffusionAnalysis".equals(row.get("kind")) && "completed".equals(row.get("state"))));
                Request remove = new Request(Map.of("graphId", "g", "runId", a.id)); controller.clear(remove.ctx);
                assertEquals(b.id, ((Map<?, ?>)remove.json().get("result")).get("runId"));
                Request unavailable = new Request(Map.of("graphId", "g", "runId", a.id)); controller.result(unavailable.ctx); assertEquals(404, unavailable.status);
                assertFalse(Files.exists(Path.of(a.simFile))); assertTrue(Files.exists(Path.of(b.simFile)));
                assertNull(session.getDiffusion(a.id)); assertSame(b, session.getDiffusion());
                Request missing = new Request(Map.of("graphId", "g", "runId", a.id, "entity", "nodes")); controller.rankings(missing.ctx); assertEquals(404, missing.status);
                simulate(controller, adapter, 1); DiffusionResult c = session.getDiffusion();
                Request latest = new Request(Map.of("graphId", "g", "runId", c.id)); controller.clear(latest.ctx);
                assertEquals(200, latest.status); assertSame(b, session.getDiffusion());
                assertEquals(b.id, ((Map<?, ?>)latest.json().get("result")).get("runId"));
                assertFalse(Files.exists(Path.of(c.simFile))); assertTrue(Files.exists(Path.of(b.simFile)));
                Request last = new Request(Map.of("graphId", "g", "runId", b.id)); controller.clear(last.ctx);
                assertTrue(last.json().containsKey("result")); assertNull(last.json().get("result")); assertNull(session.getDiffusion());
                assertFalse(Files.exists(Path.of(b.simFile)));
                simulate(controller, adapter, 1); DiffusionResult d = session.getDiffusion();
                store.remove("g"); assertFalse(Files.exists(Path.of(d.simFile)));
            } finally { session.setDiffusion(null); }
        }
    }

    @Test public void rankingsCountUniqueMembershipsAndAgreeWithNodeTimelines() throws Exception {
        try (JobManager jobs = new JobManager()) {
            GraphStore store = new GraphStore(); GraphSession session = graph(store);
            DiffusionController controller = new DiffusionController(store, jobs); ProcessController adapter = new ProcessController(jobs);
            try {
                simulate(controller, adapter, 4); String runId = session.getDiffusion().id;
                for (String category : List.of("received", "read", "propagated", "discarded")) {
                    Request nodes = new Request(Map.of("graphId", "g", "runId", runId, "entity", "nodes", "category", category)); controller.rankings(nodes.ctx);
                    Request pieces = new Request(Map.of("graphId", "g", "runId", runId, "entity", "pieces", "category", category)); controller.rankings(pieces.ctx);
                    Map<?, ?> nodeCounts = (Map<?, ?>) nodes.json().get("values"), pieceCounts = (Map<?, ?>) pieces.json().get("values");
                    assertEquals(3, nodeCounts.size()); assertEquals(3, pieceCounts.size());
                    assertEquals(nodeCounts.values().stream().mapToInt(x -> ((Number)x).intValue()).sum(), pieceCounts.values().stream().mapToInt(x -> ((Number)x).intValue()).sum());
                    Request trajectory = new Request(Map.of("graphId", "g", "runId", runId, "node", "a", "pieces", true)); controller.trajectory(trajectory.ctx);
                    List<?> values = (List<?>) ((Map<?, ?>) trajectory.json().get("series")).get(category);
                    assertEquals(((Number)nodeCounts.get("a")).doubleValue(), ((Number)values.get(values.size() - 1)).doubleValue(), 0.0);
                }
            } finally { session.setDiffusion(null); }
        }
    }

    @Test public void mixedDistributionsKeepEveryObservedPairWithoutADenseCartesianMatrix() throws Exception {
        java.lang.reflect.Method build = DiffusionController.class.getDeclaredMethod("buildMixedDistribution", Map.class, List.class, String.class, String.class);
        build.setAccessible(true);
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        for (int i = 0; i < 20; i++) lines.add("info" + i + "\tuser" + i + "\t" + (i + 1));
        build.invoke(null, out, lines, "topic", "group");
        assertEquals(20, ((List<?>)out.get("infoValues")).size());
        assertEquals(20, ((List<?>)out.get("userValues")).size());
        assertEquals(20, ((List<?>)out.get("points")).size());
        assertFalse(out.containsKey("matrix"));
        double total = ((List<?>)out.get("points")).stream().mapToDouble(point -> ((Number)((List<?>)point).get(2)).doubleValue()).sum();
        assertEquals(210.0, total, 0.0);
    }

    @Test public void exactFeatureSelectionWorksBeyondTheDisplayedTopValuesForNodesAndPieces() throws Exception {
        FastDirectedWeightedGraph<String> graph = new FastDirectedWeightedGraph<>();
        graph.defineNodeAttribute("group", es.uam.eps.ir.relison.graph.attributes.AttributeType.STRING);
        java.util.ArrayList<Map<String, Object>> pieces = new java.util.ArrayList<>();
        Map<String, java.util.Set<String>> received = new java.util.LinkedHashMap<>();
        for (int i = 0; i < 12; i++) {
            graph.addNode("n" + i); graph.setNodeAttribute("n" + i, "group", "g" + i);
            pieces.add(Map.of("id", "p" + i, "creator", "n0", "timestamp", 0L,
                    "features", List.of(Map.of("param", "topic", "value", "v" + i, "weight", 1.0))));
            received.put("p" + i, java.util.Set.of("n0"));
        }
        es.uam.eps.ir.relison.diffusion.data.Data<String, String, String> data = DiffusionData.fromPieces(graph, pieces, Map.of(), List.of());
        es.uam.eps.ir.relison.diffusion.simulation.SimpleIteration<String, String, String> iteration = new es.uam.eps.ir.relison.diffusion.simulation.SimpleIteration<>(0);
        for (int i = 0; i < 12; i++) iteration.addReceivingUser("n" + i, received);
        String file = Path.of(System.getProperty("java.io.tmpdir"), "relison-feature-test-" + UUID.randomUUID() + ".bin").toString();
        StreamingSimulationConsumer stream = new StreamingSimulationConsumer(data, file);
        try {
            stream.start(0); stream.accept(iteration);
        } finally { stream.finish(); }
        GraphStore store = new GraphStore(); GraphSession session = new GraphSession("g", graph, true, true, false, false); store.register(session);
        session.setDiffusion(new DiffusionResult(data, file, 1, stream.getSummaries(), Map.of(), Map.of()));
        try (JobManager jobs = new JobManager()) {
            DiffusionController controller = new DiffusionController(store, jobs);
            for (boolean node : List.of(true, false)) {
                Map<String, Object> body = new java.util.LinkedHashMap<>(Map.of("graphId", "g", "runId", session.getDiffusion().id,
                        node ? "node" : "piece", node ? "n0" : "p0", "feature", node ? "topic" : "group", "category", "received"));
                Request full = new Request(body); if (node) controller.trajectory(full.ctx); else controller.pieceTrajectory(full.ctx);
                List<?> displayed = (List<?>)full.json().get("values"), available = (List<?>)full.json().get("availableValues");
                assertEquals(12, available.size());
                String chosen = available.stream().map(Object::toString).filter(value -> !displayed.contains(value)).findFirst().orElseThrow();
                body.put("singleValue", true); body.put("value", chosen);
                Request exact = new Request(body); if (node) controller.trajectory(exact.ctx); else controller.pieceTrajectory(exact.ctx);
                assertEquals(List.of(chosen), exact.json().get("values")); assertEquals(chosen, exact.json().get("selectedValue"));
                assertEquals(List.of(1.0), ((Map<?, ?>)exact.json().get("series")).get(chosen));
                body.put("value", "absent");
                Request absent = new Request(body); if (node) controller.trajectory(absent.ctx); else controller.pieceTrajectory(absent.ctx);
                assertEquals(List.of(0.0), ((Map<?, ?>)absent.json().get("series")).get("absent"));
            }
            Request all = new Request(Map.of("graphId", "g", "runId", session.getDiffusion().id,
                    "feature", "topic", "category", "received", "allValues", true));
            controller.featureTrajectory(all.ctx);
            assertEquals(12, ((List<?>)all.json().get("values")).size());
            assertFalse(((List<?>)all.json().get("values")).contains("other"));
            assertEquals(List.of(1.0), ((Map<?, ?>)all.json().get("series")).get("v11"));
        } finally { session.setDiffusion(null); }
    }
}
