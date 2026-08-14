/*
 *  Copyright (C) 2023 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.gui;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Runs cancellable, long-running computations (metrics, community detection, recommendation, diffusion) on worker
 * threads so that a <em>separate</em> HTTP request can stop them while they are in progress.
 *
 * <p>A request handler calls {@link #run(String, Callable)}, which submits the work to a worker thread and blocks on
 * its {@link Future}. A concurrent call to {@link #cancel(String)} (served by another request thread) cancels that
 * future: {@code Future.cancel(true)} both unblocks the waiting handler <em>immediately</em> — so the UI becomes
 * responsive at once, even for computations that do not check for interruption — and interrupts the worker thread, so
 * the computations that <em>do</em> honour {@link Thread#isInterrupted() interruption} (Louvain, Girvan-Newman, the
 * diffusion per-iteration consumer) stop promptly and free the CPU. Computation that ignores interruption keeps
 * running in the background until it finishes on its own, but its result is discarded (the handler already returned a
 * "cancelled" response), so it can never affect the session.</p>
 *
 * <p>Jobs are keyed by {@code graphId + ":" + kind}, matching the one run button per family in the UI; starting a new
 * job of the same kind while an old one is still registered simply replaces the entry.</p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public final class JobManager
{
    /** Worker pool: daemon threads so a pending computation never keeps the JVM alive. */
    private final ExecutorService pool = Executors.newCachedThreadPool(r ->
    {
        Thread t = new Thread(r, "relison-job");
        t.setDaemon(true);
        return t;
    });

    /** The in-flight job for each key ({@code graphId:kind}). */
    private final ConcurrentHashMap<String, Future<?>> running = new ConcurrentHashMap<>();

    /**
     * Runs a computation on a worker thread and blocks the caller until it finishes or is cancelled.
     *
     * @param key  the job key ({@code graphId + ":" + kind}); a concurrent {@link #cancel(String)} with the same key
     *             stops this job.
     * @param task the computation to run.
     * @param <T>  the result type.
     * @return the computed result.
     * @throws JobCancelledException if the job was cancelled via {@link #cancel(String)}.
     * @throws Exception             any exception thrown by the computation itself.
     */
    public <T> T run(String key, Callable<T> task) throws Exception
    {
        Future<T> future = pool.submit(task);
        // Replace any stale entry for this key (e.g. a previous job whose handler already returned). We only ever
        // remove our own future below, so a racing new job of the same kind is not clobbered here.
        running.put(key, future);
        try
        {
            return future.get();
        }
        catch (CancellationException e)
        {
            throw new JobCancelledException();
        }
        catch (ExecutionException e)
        {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw e;
        }
        finally
        {
            running.remove(key, future);
        }
    }

    /**
     * Cancels the in-flight job for the given key, if any.
     * @param key the job key ({@code graphId + ":" + kind}).
     * @return {@code true} if a running job was found and cancelled, {@code false} if there was nothing to cancel.
     */
    public boolean cancel(String key)
    {
        Future<?> future = running.get(key);
        if (future == null) return false;
        return future.cancel(true);
    }
}
