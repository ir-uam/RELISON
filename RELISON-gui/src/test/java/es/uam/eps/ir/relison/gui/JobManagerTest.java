package es.uam.eps.ir.relison.gui;

import es.uam.eps.ir.relison.utils.ComputationMonitor;
import org.junit.Test;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

public class JobManagerTest
{
    private Map<String, Object> job(JobManager manager, String id) {
        return manager.snapshots().stream().filter(row -> id.equals(row.get("id"))).findFirst().orElseThrow();
    }
    private void await(CountDownLatch latch) throws Exception { assertTrue(latch.await(5, TimeUnit.SECONDS)); }
    private void awaitState(JobManager manager, String id, String expected) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            Optional<Map<String, Object>> current = manager.snapshots().stream()
                    .filter(row -> id.equals(row.get("id"))).findFirst();
            if (current.isPresent() && expected.equals(current.get().get("state"))) return;
            Thread.sleep(10);
        }
        assertEquals(expected, job(manager, id).get("state"));
    }
    private CompletableFuture<Boolean> run(JobManager manager, String id, String graph, Callable<?> action) {
        return CompletableFuture.supplyAsync(() -> {
            try { manager.run(id, graph, "metric", "A metric", Map.of(), action); return false; }
            catch (JobCancelledException expected) { return true; }
            catch (Exception error) { throw new CompletionException(error); }
        });
    }

    @Test public void cancelledWorkerRemainsVisibleAndProtectsGraphUntilItExits() throws Exception {
        try (JobManager manager = new JobManager()) {
            String id = UUID.randomUUID().toString();
            CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
            AtomicBoolean published = new AtomicBoolean();
            CompletableFuture<Boolean> result = run(manager, id, "graph", () -> {
                ComputationMonitor.current().progress("Nodes", 2, 10, "nodes"); started.countDown();
                // Model a numerical call that ignores interruption until it returns.
                boolean waiting = true;
                while (waiting) { try { release.await(); waiting = false; } catch (InterruptedException ignored) { } }
                JobManager.publish(() -> published.set(true)); return null;
            });
            try {
                await(started); assertTrue(manager.cancel(id, "graph"));
                Map<String, Object> cancelling = job(manager, id);
                assertEquals("cancelling", cancelling.get("state")); assertEquals(0L, cancelling.get("endedAt"));
                assertEquals(2L, cancelling.get("completed")); assertFalse(result.isDone());
                assertFalse(manager.tryMutation("graph", () -> fail("Must not edit a graph still in use")));
                assertTrue(manager.tryMutation("other", () -> { }));
            } finally { release.countDown(); }
            assertTrue(result.get(5, TimeUnit.SECONDS)); assertFalse(published.get());
            assertEquals("cancelled", job(manager, id).get("state"));
            assertTrue((Long) job(manager, id).get("endedAt") > 0);
            assertTrue(manager.tryMutation("graph", () -> { }));
        }
    }

    @Test public void sameGraphQueuesWhileDifferentGraphsCanRun() throws Exception {
        try (JobManager manager = new JobManager()) {
            CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1), other = new CountDownLatch(1);
            String first = UUID.randomUUID().toString(), queued = UUID.randomUUID().toString(), third = UUID.randomUUID().toString();
            CompletableFuture<Boolean> a = run(manager, first, "graph", () -> { started.countDown(); release.await(); return null; });
            try {
                await(started);
                CompletableFuture<Boolean> b = run(manager, queued, "graph", () -> { fail("Cancelled queued work must not execute"); return null; });
                CompletableFuture<Boolean> c = run(manager, third, "other", () -> { other.countDown(); return null; });
                await(other); assertFalse(c.get(5, TimeUnit.SECONDS));
                // Cancelling by id also covers arrival before the request is registered.
                assertTrue(manager.cancel(queued, "graph")); assertTrue(b.get(5, TimeUnit.SECONDS));
                assertEquals("cancelled", job(manager, queued).get("state"));
            } finally { release.countDown(); }
            assertFalse(a.get(5, TimeUnit.SECONDS));
        }
    }

    @Test public void publicationWinsAtomicallyAndCannotBeCancelledAfterwards() throws Exception {
        try (JobManager manager = new JobManager()) {
            String id = UUID.randomUUID().toString();
            CountDownLatch committed = new CountDownLatch(1), release = new CountDownLatch(1);
            CompletableFuture<Boolean> result = run(manager, id, "graph", () -> {
                JobManager.publish(() -> { }); committed.countDown(); release.await(); return null;
            });
            try { await(committed); assertFalse(manager.cancel(id, "graph")); }
            finally { release.countDown(); }
            assertFalse(result.get(5, TimeUnit.SECONDS)); assertEquals("completed", job(manager, id).get("state"));
        }
    }

    @Test public void earlyCancellationErrorsAndBoundedHistory() throws Exception {
        try (JobManager manager = new JobManager()) {
            String id = UUID.randomUUID().toString();
            assertTrue(manager.cancel(id, "graph"));
            assertTrue(run(manager, id, "graph", () -> { fail("Early cancellation must suppress execution"); return null; }).get(5, TimeUnit.SECONDS));
            String failure = UUID.randomUUID().toString();
            try { manager.run(failure, "graph", "metric", "Broken", Map.of(), () -> { throw new IllegalStateException("deliberate failure"); }); fail(); }
            catch (IllegalStateException expected) { assertEquals("deliberate failure", expected.getMessage()); }
            assertEquals("failed", job(manager, failure).get("state"));
            assertEquals("deliberate failure", job(manager, failure).get("error"));
            for (int i = 0; i < 105; i++) manager.run("graph:metric", () -> 1);
            assertEquals(100, manager.snapshots().size());
            assertSame(ComputationMonitor.NONE, ComputationMonitor.current());
        }
    }

    @Test public void linkageErrorFailsOnlyItsJobAndReleasesTheGraphQueue() throws Exception {
        try (JobManager manager = new JobManager()) {
            String brokenId = UUID.randomUUID().toString(), nextId = UUID.randomUUID().toString();
            CountDownLatch brokenStarted = new CountDownLatch(1), releaseBroken = new CountDownLatch(1);
            CompletableFuture<Void> broken = CompletableFuture.runAsync(() -> {
                try {
                    manager.run(brokenId, "graph", "metric", "Broken metric", Map.of(), () -> {
                        brokenStarted.countDown(); releaseBroken.await();
                        throw new NoClassDefFoundError("missing metric selector");
                    });
                    fail("The broken request should fail");
                } catch (NoClassDefFoundError expected) { }
                catch (Exception error) { throw new CompletionException(error); }
            });
            try {
                await(brokenStarted);
                CompletableFuture<Boolean> next = run(manager, nextId, "graph", () -> true);
                awaitState(manager, nextId, "queued");
                releaseBroken.countDown();
                broken.get(5, TimeUnit.SECONDS);
                assertFalse(next.get(5, TimeUnit.SECONDS));
                assertEquals("failed", job(manager, brokenId).get("state"));
                assertEquals("completed", job(manager, nextId).get("state"));
            } finally { releaseBroken.countDown(); }
        }
    }
}
