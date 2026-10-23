/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.gui;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.viz.*;
import io.javalin.http.Context;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Bounded, expiring force sessions advanced by browser frame requests. */
public final class AnimatedLayoutController {
    private final GraphStore store;
    private final Map<String, Entry> sessions = new ConcurrentHashMap<>();
    private static final long IDLE_NANOS = 120_000_000_000L;
    private static final int MAX_SESSIONS = 128;
    private static final class Entry {
        final LayoutController.Prepared prepared;
        final LayoutSession<String> session;
        final AtomicBoolean cancelled;
        volatile long touched = System.nanoTime();
        Entry(LayoutController.Prepared prepared, LayoutSession<String> session, AtomicBoolean cancelled) {
            this.prepared = prepared; this.session = session; this.cancelled = cancelled;
        }
    }
    /** @param store graph registry */
    public AnimatedLayoutController(GraphStore store) { this.store = Objects.requireNonNull(store); }

    /** @param ctx request with the same fields as batch layout */
    public void start(Context ctx) {
        try {
            Map<?, ?> body = ctx.bodyAsClass(Map.class);
            if (body == null) throw new IllegalArgumentException("A layout request is required.");
            GraphSession graph = store.get(String.valueOf(body.get("graphId")));
            if (graph == null) { ctx.status(404).json(Map.of("error", "Unknown graph id.")); return; }
            ctx.json(start(graph.getGraph(), body));
        } catch (IllegalArgumentException error) { badRequest(ctx, error); }
        catch (IllegalStateException error) { ctx.status(429).json(Map.of("error", error.getMessage())); }
    }

    /** @param ctx session identifier and iteration count */
    public void step(Context ctx) {
        try {
            Map<?, ?> body = ctx.bodyAsClass(Map.class);
            Object count = body == null ? null : body.get("iterations");
            if (!(count instanceof Number) || !Double.isFinite(((Number) count).doubleValue())
                || ((Number) count).doubleValue() != ((Number) count).intValue())
                throw new IllegalArgumentException("iterations must be an integer between 1 and 10.");
            ctx.json(advance(ctx.pathParam("id"), ((Number) count).intValue()));
        } catch (NoSuchElementException error) { ctx.status(404).json(Map.of("error", error.getMessage())); }
        catch (IllegalArgumentException error) { badRequest(ctx, error); }
    }

    /** Cancels immediately without waiting for the stepping thread.
     * @param ctx session identifier
     */
    public void cancel(Context ctx) { ctx.json(Map.of("cancelled", cancel(ctx.pathParam("id")))); }

    private static void badRequest(Context ctx, IllegalArgumentException error) {
        ctx.status(400).json(Map.of("error", error.getMessage() == null ? "Invalid layout request." : error.getMessage()));
    }

    synchronized Map<String, Object> start(Graph<String> graph, Map<?, ?> body) {
        reap();
        if (sessions.size() >= MAX_SESSIONS) throw new IllegalStateException("Too many active layout sessions.");
        AtomicBoolean cancelled = new AtomicBoolean();
        LayoutController.Prepared prepared = LayoutController.prepare(graph, body, cancelled::get);
        if (!(prepared.layout instanceof IterativeLayout)) throw new IllegalArgumentException("This layout is not iterative.");
        @SuppressWarnings("unchecked")
        IterativeLayout<String> layout = (IterativeLayout<String>) prepared.layout;
        Entry entry = new Entry(prepared, layout.initialize(graph, prepared.request), cancelled);
        String id = UUID.randomUUID().toString();
        sessions.put(id, entry);
        try { return frame(id, entry); }
        catch (RuntimeException error) { cancel(id); throw error; }
    }

    Map<String, Object> advance(String id, int iterations) {
        if (iterations < 1 || iterations > 10) throw new IllegalArgumentException("Frame iterations must be between 1 and 10.");
        reap();
        Entry entry = sessions.get(id);
        if (entry == null) throw new NoSuchElementException("Layout session ended or expired.");
        synchronized (entry) {
            if (sessions.get(id) != entry) throw new NoSuchElementException("Layout session ended or expired.");
            entry.touched = System.nanoTime();
            try { entry.session.step(iterations); return frame(id, entry); }
            catch (RuntimeException error) { cancel(id); throw error; }
        }
    }

    boolean cancel(String id) {
        Entry entry = sessions.remove(id);
        if (entry == null) return false;
        entry.cancelled.set(true);
        entry.session.cancel();
        return true;
    }

    private Map<String, Object> frame(String id, Entry entry) {
        LayoutResult<String> result = entry.session.snapshot();
        boolean finished = entry.session.isFinished();
        if (finished) {
            if (result.getDiagnostics().getTermination() != LayoutDiagnostics.Termination.CANCELLED)
                result = entry.prepared.finish(result);
            sessions.remove(id, entry);
        }
        Map<String, Object> frame = new LinkedHashMap<>(LayoutController.response(result));
        frame.put("sessionId", id); frame.put("finished", finished);
        return frame;
    }

    private void reap() {
        long now = System.nanoTime();
        sessions.forEach((id, entry) -> { if (now - entry.touched > IDLE_NANOS) cancel(id); });
    }
}
