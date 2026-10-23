package es.uam.eps.ir.relison.gui;

import es.uam.eps.ir.relison.utils.ComputationMonitor;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/** Registry of actual worker lifetimes, including cancellation and cleanup. */
public final class JobManager implements AutoCloseable
{
    private static final int HISTORY_LIMIT = 100;
    private final ExecutorService pool = Executors.newFixedThreadPool(4, r -> {
        Thread thread = new Thread(r, "relison-job"); thread.setDaemon(true); return thread;
    });
    private final ConcurrentHashMap<String, Job> jobs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> graphLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SerialExecutor> graphQueues = new ConcurrentHashMap<>();
    // Stop can arrive before its computation request reaches the server.
    private final Map<String, Long> earlyCancels = new LinkedHashMap<>();
    private static final ThreadLocal<Job> CURRENT = new ThreadLocal<>();

    /** Runs at most one graph job at a time without occupying workers for queued jobs. */
    private static final class SerialExecutor implements Executor {
        private final Executor executor;
        private final Deque<Runnable> tasks = new ArrayDeque<>();
        private Runnable active;
        SerialExecutor(Executor executor) { this.executor = executor; }
        @Override public synchronized void execute(Runnable command) {
            tasks.addLast(() -> {
                try { command.run(); }
                finally { scheduleNext(); }
            });
            if (active == null) scheduleNext();
        }
        private synchronized void scheduleNext() {
            active = tasks.pollFirst();
            if (active != null) {
                try { executor.execute(active); }
                catch (RejectedExecutionException error) { active = null; throw error; }
            }
        }
    }

    private static final class Job implements ComputationMonitor
    {
        final String id, graphId, kind, label;
        final Map<String, Object> parameters;
        final long createdAt = System.currentTimeMillis(), createdNanos = System.nanoTime();
        long startedAt, startedNanos, endedAt, endedNanos;
        String state = "queued", phase = "Waiting for a worker", unit = "", error;
        long completed, total = -1;
        final LinkedHashMap<String, Progress> progressHistory = new LinkedHashMap<>();
        volatile boolean cancelled;
        boolean committed;
        Thread worker;
        Future<?> future;
        private static final class Progress {
            final String phase, unit;
            long completed, total;
            boolean done;
            Progress(String phase, long completed, long total, String unit) {
                this.phase = phase; this.completed = completed; this.total = total; this.unit = unit;
            }
            Map<String, Object> snapshot() {
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("phase", phase); out.put("completed", completed); out.put("total", total);
                out.put("unit", unit); out.put("done", done);
                return out;
            }
        }
        Job(String id, String graphId, String kind, String label, Map<String, Object> parameters) {
            this.id = id; this.graphId = graphId; this.kind = kind;
            this.label = label; this.parameters = Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
        }
        synchronized boolean terminal() { return endedAt != 0; }
        synchronized boolean cancel() {
            if (terminal() || committed) return false;
            if (cancelled) return true;
            cancelled = true; state = "cancelling";
            if (worker != null) worker.interrupt();
            else {
                state = "cancelled"; endedAt = System.currentTimeMillis(); endedNanos = System.nanoTime();
                if (future != null) { future.cancel(false); future = null; }
            }
            return true;
        }
        @Override public void checkCancelled() {
            if (cancelled || Thread.currentThread().isInterrupted()) throw new CancellationException();
        }
        @Override public synchronized void progress(String phase, long completed, long total, String unit) {
            if (!Objects.equals(this.phase, phase)) {
                Progress previous = progressHistory.values().stream().reduce((first, second) -> second).orElse(null);
                if (previous != null) previous.done = true;
            }
            checkCancelled(); this.phase = phase; this.completed = completed;
            this.total = total; this.unit = unit;
            if (total >= 0 || (unit != null && !unit.isBlank())) {
                String key = phase + "\u0000" + (unit == null ? "" : unit);
                Progress progress = progressHistory.computeIfAbsent(key, ignored -> new Progress(phase, completed, total, unit));
                progress.completed = completed; progress.total = total; progress.done = false;
            }
        }
        synchronized void finishProgress() {
            if (progressHistory.isEmpty()) {
                Progress complete = new Progress("Completed", 1, 1, ""); complete.done = true;
                progressHistory.put("completed", complete);
            } else progressHistory.values().forEach(progress -> progress.done = true);
        }
        synchronized Map<String, Object> snapshot() {
            long now = endedAt == 0 ? System.nanoTime() : endedNanos;
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("id", id); out.put("graphId", graphId); out.put("kind", kind);
            out.put("label", label); out.put("parameters", parameters); out.put("state", state);
            out.put("phase", phase); out.put("completed", completed); out.put("total", total);
            out.put("progresses", progressHistory.values().stream().map(Progress::snapshot).toList());
            out.put("unit", unit); out.put("createdAt", createdAt); out.put("startedAt", startedAt);
            out.put("endedAt", endedAt); out.put("elapsedMillis", startedAt == 0 ? 0 : (now - startedNanos) / 1_000_000);
            out.put("queueMillis", ((startedAt == 0 ? now : startedNanos) - createdNanos) / 1_000_000);
            out.put("cancellable", !terminal() && !committed && !cancelled); out.put("error", error);
            return out;
        }
    }

    /** Legacy nested calls execute in the registered request's worker. */
    public <T> T run(String key, Callable<T> task) throws Exception {
        Job current = CURRENT.get();
        if (current != null) { current.checkCancelled(); T result = task.call(); current.checkCancelled(); return result; }
        int split = key.lastIndexOf(':');
        return run(UUID.randomUUID().toString(), key.substring(0, split), key.substring(split + 1),
                key.substring(split + 1), Map.of(), task);
    }

    public <T> T run(String id, String graphId, String kind, String label,
                     Map<String, Object> parameters, Callable<T> task) throws Exception {
        if (pool.isShutdown()) throw new IllegalStateException("The process manager is shutting down.");
        Job job = new Job(id, graphId, kind, label, parameters);
        synchronized (earlyCancels) {
            if (jobs.putIfAbsent(id, job) != null) throw new IllegalArgumentException("Duplicate process id.");
            if (earlyCancels.remove(graphId + ":" + id) != null) job.cancel();
        }
        prune();
        FutureTask<T> future = new FutureTask<>(() -> {
            ReentrantLock lock = graphLocks.computeIfAbsent(graphId, ignored -> new ReentrantLock(true));
            boolean locked = false;
            synchronized (job) { job.worker = Thread.currentThread(); }
            CURRENT.set(job);
            try (ComputationMonitor.Scope scope = ComputationMonitor.open(job)) {
                job.checkCancelled();
                synchronized (job) { job.phase = "Waiting for other processes on this graph"; }
                lock.lockInterruptibly(); locked = true;
                synchronized (job) {
                    job.checkCancelled(); job.startedAt = System.currentTimeMillis();
                    job.startedNanos = System.nanoTime(); job.state = "running"; job.phase = "Preparing";
                }
                T result = task.call();
                synchronized (job) { job.checkCancelled(); job.state = job.error == null ? "completed" : "failed"; if (job.state.equals("completed")) job.finishProgress(); job.committed = true; }
                return result;
            } catch (Throwable error) {
                synchronized (job) {
                    if (job.cancelled || error instanceof CancellationException || error instanceof InterruptedException) {
                        job.state = "cancelled"; throw new JobCancelledException();
                    }
                    job.state = "failed"; job.error = error.getMessage() == null ? error.toString() : error.getMessage();
                }
                if (error instanceof Exception) throw (Exception) error;
                throw (Error) error;
            } finally {
                if (locked) lock.unlock();
                synchronized (job) { job.worker = null; job.future = null; job.endedAt = System.currentTimeMillis(); job.endedNanos = System.nanoTime(); }
                CURRENT.remove(); Thread.interrupted(); prune();
            }
        });
        synchronized (job) { job.future = future; if (job.cancelled) { future.cancel(false); job.future = null; } }
        try { graphQueues.computeIfAbsent(graphId, ignored -> new SerialExecutor(pool)).execute(future); }
        catch (RejectedExecutionException error) {
            synchronized (job) { job.state = "failed"; job.error = error.toString();
                job.future = null; job.endedAt = System.currentTimeMillis(); job.endedNanos = System.nanoTime(); }
            prune(); throw error;
        }
        try { return future.get(); }
        catch (CancellationException error) { throw new JobCancelledException(); }
        catch (ExecutionException error) {
            Throwable cause = error.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            throw (Error) cause;
        } catch (InterruptedException error) { job.cancel(); Thread.currentThread().interrupt(); throw error; }
    }

    /** Accepted cancellation can never publish a result. */
    public static void publish(Runnable action) {
        Job job = CURRENT.get();
        if (job == null) { action.run(); return; }
        synchronized (job) { job.checkCancelled(); action.run(); job.committed = true; }
    }
    public static void fail(String message) {
        Job job = CURRENT.get();
        if (job != null) { synchronized (job) { job.error = message; } }
    }
    public boolean cancel(String key) {
        boolean found = false;
        for (Job job : jobs.values()) if ((job.graphId + ":" + job.kind).equals(key)) found |= job.cancel();
        return found;
    }
    public boolean cancel(String id, String graphId) {
        synchronized (earlyCancels) {
            Job job = jobs.get(id);
            if (job != null) return job.graphId.equals(graphId) && job.cancel();
            UUID.fromString(id);
            long now = System.currentTimeMillis();
            earlyCancels.entrySet().removeIf(entry -> now - entry.getValue() > 60_000);
            if (earlyCancels.size() >= HISTORY_LIMIT) earlyCancels.remove(earlyCancels.keySet().iterator().next());
            earlyCancels.put(graphId + ":" + id, now); return true;
        }
    }
    public List<Map<String, Object>> snapshots() {
        List<Map<String, Object>> out = new ArrayList<>();
        jobs.values().forEach(job -> out.add(job.snapshot()));
        out.sort(Comparator.comparingLong(job -> -((Number) job.get("createdAt")).longValue()));
        return out;
    }
    /** Edits fail fast while any worker is reading this graph, including cleanup. */
    @FunctionalInterface public interface CheckedAction { void run() throws Exception; }
    public boolean tryMutation(String graphId, CheckedAction action) throws Exception {
        ReentrantLock lock = graphLocks.computeIfAbsent(graphId, ignored -> new ReentrantLock(true));
        if (!lock.tryLock()) return false;
        try { action.run(); return true; } finally { lock.unlock(); }
    }
    private void prune() {
        List<Job> finished = new ArrayList<>();
        for (Job job : jobs.values()) if (job.terminal()) finished.add(job);
        finished.sort(Comparator.comparingLong(job -> job.endedAt));
        for (int i = 0; i < finished.size() - HISTORY_LIMIT; i++) jobs.remove(finished.get(i).id, finished.get(i));
    }
    @Override public void close() { jobs.values().forEach(Job::cancel); pool.shutdown(); }
}
