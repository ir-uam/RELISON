package es.uam.eps.ir.relison.gui;

import io.javalin.Javalin;
import org.junit.Test;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public class ProcessControllerTest
{
    private HttpClient client;
    private HttpRequest post(String url, String body, String id) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        if (id != null) builder.header("X-Relison-Job-ID", id);
        return builder.build();
    }
    @Test public void httpCancellationKeepsTheRequestPendingUntilCleanupAndProtectsEdits() throws Exception {
        try { client = HttpClient.newHttpClient(); }
        catch (java.io.UncheckedIOException error) {
            // Windows' sandbox can deny the loopback pipe used by HttpClient itself.
            Throwable cause = error;
            while (cause.getCause() != null) cause = cause.getCause();
            if (cause instanceof java.net.SocketException && cause.getMessage().contains("Permission denied"))
                org.junit.Assume.assumeNoException("Localhost networking is unavailable in this sandbox", error);
            throw error;
        }
        try (JobManager manager = new JobManager()) {
            ProcessController processes = new ProcessController(manager);
            CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1), cleanup = new CountDownLatch(1);
            Javalin app = Javalin.create(config -> {
                config.routes.get("/api/jobs", ctx -> ctx.json(Map.of("jobs", manager.snapshots())));
                config.routes.post("/compute", processes.track("vertex", ctx -> {
                    started.countDown();
                    try { release.await(); } finally { cleanup.countDown(); }
                    ctx.json(Map.of("value", 42));
                }));
                config.routes.post("/edit", processes.mutation(ctx -> ctx.json(Map.of("edited", true))));
                config.routes.post("/cancel", ctx -> {
                    Map<?, ?> body = ctx.bodyAsClass(Map.class);
                    ctx.json(Map.of("cancelled", manager.cancel(String.valueOf(body.get("jobId")), String.valueOf(body.get("graphId")))));
                });
                config.routes.post("/fail", processes.track("vertex", ctx -> ctx.status(400).json(Map.of("error", "Bad metric"))));
            }).start(0);
            try {
                String url = "http://localhost:" + app.port(), id = UUID.randomUUID().toString();
                CompletableFuture<HttpResponse<String>> computation = client.sendAsync(post(url + "/compute", "{\"graphId\":\"graph\",\"metric\":\"Degree\"}", id), HttpResponse.BodyHandlers.ofString());
                assertTrue(started.await(5, TimeUnit.SECONDS));
                assertEquals(409, client.send(post(url + "/edit", "{\"graphId\":\"graph\"}", null), HttpResponse.BodyHandlers.ofString()).statusCode());
                String snapshot = client.send(HttpRequest.newBuilder(URI.create(url + "/api/jobs")).GET().build(), HttpResponse.BodyHandlers.ofString()).body();
                assertTrue(snapshot.contains(id)); assertTrue(snapshot.contains("Degree"));
                HttpResponse<String> cancellation = client.send(post(url + "/cancel", "{\"graphId\":\"graph\",\"jobId\":\"" + id + "\"}", null), HttpResponse.BodyHandlers.ofString());
                assertEquals(200, cancellation.statusCode());
                HttpResponse<String> response = computation.get(5, TimeUnit.SECONDS);
                assertEquals(200, response.statusCode()); assertTrue(response.body().contains("\"cancelled\":true"));
                assertEquals(0, cleanup.getCount());
                assertEquals(200, client.send(post(url + "/edit", "{\"graphId\":\"graph\"}", null), HttpResponse.BodyHandlers.ofString()).statusCode());
                String failureId = UUID.randomUUID().toString();
                assertEquals(400, client.send(post(url + "/fail", "{\"graphId\":\"graph\"}", failureId), HttpResponse.BodyHandlers.ofString()).statusCode());
                assertEquals("failed", manager.snapshots().stream().filter(row -> failureId.equals(row.get("id"))).findFirst().orElseThrow().get("state"));
            } finally { release.countDown(); app.stop(); }
        }
    }
}
