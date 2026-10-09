/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.internal;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.layouts.StressMajorizationConfig;
import java.util.*;

/**
 * Full-stress majorization with constrained, reusable Cholesky systems.
 * @param <U> node type
 */
public final class StressMajorizationSession<U> extends NumericalLayoutSession<U>
{
    private final StressMajorizationConfig config;
    private double[][] target, weight;
    private List<SystemBlock> systems;
    /**
     * @param input graph
     * @param request constraints
     * @param config model settings
     */
    public StressMajorizationSession(Graph<U> input, LayoutRequest<U> request, StressMajorizationConfig config)
    { super(input, request, "stress-majorization", config.edgeLength); this.config = config; }

    private void prepare()
    {
        int n = x.length;
        double[][] distances = GraphDistances.compute(graph, config.weighted, this::checkpoint);
        double[][] targets = new double[n][n], weights = new double[n][n];
        List<SystemBlock> blocks = new ArrayList<>();
        for (int[] component : Topology.components(graph))
        {
            checkpoint();
            boolean[] fixed = pins.clone();
            boolean hasPin = false;
            for (int v : component) hasPin |= pins[v];
            if (!hasPin) fixed[component[0]] = true;
            int[] free = Arrays.stream(component).filter(v -> !fixed[v]).toArray();
            for (int i : component) for (int j : component) if (i != j)
            {
                double t = distances[i][j]*config.edgeLength, w = (1/t)/t;
                if (!Double.isFinite(t) || t <= 0 || !Double.isFinite(w) || w <= 0)
                    throw new IllegalArgumentException("Stress target distance or weight overflow/underflow");
                targets[i][j] = t; weights[i][j] = w;
            }
            double[][] factor = new double[free.length][free.length];
            for (int a = 0; a < free.length; a++)
            {
                checkpoint();
                int i = free[a];
                for (int j : component) factor[a][a] += weights[i][j];
                for (int b = 0; b < a; b++) factor[a][b] = -weights[i][free[b]];
            }
            // Factor the reduced positive-definite Laplacian once.
            for (int a = 0; a < free.length; a++)
            {
                checkpoint();
                for (int b = 0; b <= a; b++)
                {
                    double value = factor[a][b];
                    for (int k = 0; k < b; k++) value -= factor[a][k]*factor[b][k];
                    if (a == b)
                    {
                        if (value <= 0 || !Double.isFinite(value)) throw new IllegalArgumentException("Stress system is numerically singular");
                        factor[a][b] = Math.sqrt(value);
                    }
                    else factor[a][b] = value/factor[b][b];
                }
            }
            blocks.add(new SystemBlock(component, free, fixed, factor));
        }
        // Assign only after complete preparation, so cancellation leaves a valid snapshot.
        target = targets; weight = weights; systems = blocks;
    }

    @Override protected void advance()
    {
        if (systems == null) prepare();
        double before = stress(x, y);
        double[] nextX = x.clone(), nextY = y.clone();
        for (SystemBlock block : systems)
        {
            double[] bx = new double[block.free.length], by = new double[block.free.length];
            for (int a = 0; a < block.free.length; a++)
            {
                checkpoint(); int i = block.free[a];
                for (int j : block.component) if (i != j)
                {
                    double vx = x[i]-x[j], vy = y[i]-y[j], r = Math.hypot(vx, vy);
                    // The paper defines inv(0)=0, preserving coincident warm starts.
                    double ratio = r == 0 ? 0 : weight[i][j]*target[i][j]/r;
                    bx[a] += ratio*vx; by[a] += ratio*vy;
                    if (block.fixed[j]) { bx[a] += weight[i][j]*x[j]; by[a] += weight[i][j]*y[j]; }
                }
            }
            solve(block.factor, bx); solve(block.factor, by);
            for (int a = 0; a < block.free.length; a++) { nextX[block.free[a]] = bx[a]; nextY[block.free[a]] = by[a]; }
        }
        double after = stress(nextX, nextY);
        if (after > before)
        {
            if (after-before > 1e-10*Math.max(1, before))
                throw new IllegalArgumentException("Stress solve lost monotonicity; check coordinate/weight scales");
            nextX = x.clone(); nextY = y.clone(); after = before;
        }
        commit(nextX, nextY);
        if (before == 0 || (before-after)/before <= config.tolerance)
            status = LayoutDiagnostics.Termination.CONVERGED;
    }

    private void solve(double[][] factor, double[] value)
    {
        for (int i = 0; i < value.length; i++)
        {
            checkpoint();
            for (int j = 0; j < i; j++) value[i] -= factor[i][j]*value[j];
            value[i] /= factor[i][i];
        }
        for (int i = value.length-1; i >= 0; i--)
        {
            checkpoint();
            for (int j = i+1; j < value.length; j++) value[i] -= factor[j][i]*value[j];
            value[i] /= factor[i][i];
        }
    }

    private double stress(double[] xx, double[] yy)
    {
        double result = 0;
        for (int i = 0; i < x.length; i++)
        {
            checkpoint();
            for (int j = i+1; j < x.length; j++) if (weight[i][j] > 0)
            {
                double error = (Math.hypot(xx[i]-xx[j], yy[i]-yy[j])-target[i][j])/target[i][j];
                result += error*error;
            }
        }
        if (!Double.isFinite(result)) throw new IllegalArgumentException("Stress overflow");
        return result;
    }
    private static final class SystemBlock
    {
        final int[] component, free; final boolean[] fixed; final double[][] factor;
        SystemBlock(int[] component, int[] free, boolean[] fixed, double[][] factor)
        { this.component = component; this.free = free; this.fixed = fixed; this.factor = factor; }
    }
}
