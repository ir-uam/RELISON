/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.internal;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
import es.uam.eps.ir.relison.viz.*;
import java.util.*;

/**
 * Scheduling, atomic coordinate commits and cancellation for numerical models.
 * Initialization and idle time are excluded from the active stepping budget.
 * @param <U> node type
 */
public abstract class NumericalLayoutSession<U> implements LayoutSession<U>
{
    protected final IndexedGraphSnapshot<U> graph;
    protected final LayoutRequest<U> request;
    protected final boolean[] pins;
    protected final double[] x, y;
    protected final Random random;
    protected LayoutDiagnostics.Termination status = LayoutDiagnostics.Termination.RUNNING;
    private final String id;
    private volatile boolean cancelled;
    private int iterations;
    private double movement;
    private long activeNanos, stepStarted;
    private boolean committed;

    /**
     * @param input input graph
     * @param request constraints and scheduling limits
     * @param id algorithm identifier
     * @param scale initialization scale
     */
    protected NumericalLayoutSession(Graph<U> input, LayoutRequest<U> request, String id, double scale)
    {
        this.graph = IndexedGraphSnapshot.capture(input, request);
        this.request = request; this.id = id;
        int n = graph.getNodes().size();
        x = new double[n]; y = new double[n]; pins = new boolean[n];
        random = new Random(request.getSeed());
        double side = scale * Math.sqrt(Math.max(1, n));
        if (!Double.isFinite(side)) throw new IllegalArgumentException("Initialization scale overflow");
        boolean movable = false;
        for (int i = 0; i < n; i++)
        {
            U node = graph.getNodes().get(i);
            Pair<Double> p = request.getInitialPositions().get(node);
            x[i] = p == null ? (random.nextDouble()-.5)*side : p.v1();
            y[i] = p == null ? (random.nextDouble()-.5)*side : p.v2();
            pins[i] = request.getPinnedNodes().contains(node);
            movable |= !pins[i];
        }
        if (!movable) status = LayoutDiagnostics.Termination.CONVERGED;
        else if (request.getMaxIterations() == 0) status = LayoutDiagnostics.Termination.LIMIT_REACHED;
        stopped();
    }

    /** @return whether termination was requested or a budget was exhausted */
    protected final boolean stopped()
    {
        if (cancelled || Thread.currentThread().isInterrupted() || request.getCancellation().getAsBoolean())
            status = LayoutDiagnostics.Termination.CANCELLED;
        if (status != LayoutDiagnostics.Termination.RUNNING) return true;
        long elapsed = activeNanos + (stepStarted == 0 ? 0 : System.nanoTime()-stepStarted);
        if (request.getTimeLimitMillis() > 0 && elapsed/1_000_000 >= request.getTimeLimitMillis())
            status = LayoutDiagnostics.Termination.LIMIT_REACHED;
        return status != LayoutDiagnostics.Termination.RUNNING;
    }

    /** Checks for cancellation inside expensive loops. */
    protected final void checkpoint() { if (stopped()) throw new Stopped(); }

    /** Computes one numerical iteration, committing only fully evaluated coordinates. */
    protected abstract void advance();

    /**
     * @param nextX complete candidate x coordinates
     * @param nextY complete candidate y coordinates
     */
    protected final void commit(double[] nextX, double[] nextY)
    {
        checkpoint();
        double maximum = 0;
        for (int i = 0; i < x.length; i++)
        {
            if (!Double.isFinite(nextX[i]) || !Double.isFinite(nextY[i]))
                throw new IllegalArgumentException("Numerical layout produced non-finite coordinates");
            if (pins[i] && (nextX[i] != x[i] || nextY[i] != y[i]))
                throw new IllegalStateException("Numerical layout moved a pinned node");
            maximum = Math.max(maximum, Math.hypot(nextX[i]-x[i], nextY[i]-y[i]));
        }
        if (!Double.isFinite(maximum)) throw new IllegalArgumentException("Coordinate displacement overflow");
        System.arraycopy(nextX, 0, x, 0, x.length);
        System.arraycopy(nextY, 0, y, 0, y.length);
        movement = maximum;
        committed = true;
    }

    @Override public final void step(int count)
    {
        if (count < 0) throw new IllegalArgumentException("Iteration count must be nonnegative");
        if (stopped()) return;
        stepStarted = System.nanoTime();
        try
        {
            for (int i = 0; i < count && !stopped(); i++)
            {
                committed = false;
                advance();
                iterations++;
                committed = false;
                if (status == LayoutDiagnostics.Termination.RUNNING && iterations >= request.getMaxIterations())
                    status = LayoutDiagnostics.Termination.LIMIT_REACHED;
            }
        }
        catch (Stopped ignored) { if (committed) iterations++; /* Retain the last complete iteration. */ }
        finally { activeNanos += System.nanoTime()-stepStarted; stepStarted = 0; }
    }

    @Override public final LayoutResult<U> snapshot()
    {
        stopped();
        Map<U, Pair<Double>> result = new LinkedHashMap<>();
        for (int i = 0; i < x.length; i++) result.put(graph.getNodes().get(i), new Pair<>(x[i], y[i]));
        return new LayoutResult<>(result, new LayoutDiagnostics(id, status, iterations, movement));
    }
    @Override public final boolean isFinished() { return stopped(); }
    @Override public final void cancel() { cancelled = true; }
    private static final class Stopped extends RuntimeException { private static final long serialVersionUID = 1L; }
}
