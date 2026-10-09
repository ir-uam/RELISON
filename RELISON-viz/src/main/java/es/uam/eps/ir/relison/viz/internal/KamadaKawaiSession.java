/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.internal;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.layouts.KamadaKawaiConfig;

/**
 * Original nested-loop vertex selection and analytic 2D Newton equations.
 * @param <U> node type
 */
public final class KamadaKawaiSession<U> extends NumericalLayoutSession<U>
{
    private final KamadaKawaiConfig config;
    private double[][] length, spring;
    private int selected = -1;
    /**
     * @param graph input graph
     * @param request constraints
     * @param config model parameters
     */
    public KamadaKawaiSession(Graph<U> graph, LayoutRequest<U> request, KamadaKawaiConfig config)
    { super(graph, request, "kamada-kawai", config.drawingSize/Math.sqrt(Math.max(1, graph.getVertexCount()))); this.config = config; }

    private void prepare()
    {
        double[][] d = GraphDistances.compute(graph, config.weighted, this::checkpoint);
        double[][] lengths = new double[x.length][x.length], springs = new double[x.length][x.length];
        for (int[] component : Topology.components(graph))
        {
            double diameter = 0;
            for (int i : component) for (int j : component) diameter = Math.max(diameter, d[i][j]);
            if (diameter == 0) continue;
            for (int i : component)
            {
                checkpoint();
                for (int j : component) if (i != j)
                {
                    double l = config.drawingSize*(d[i][j]/diameter), k = (config.springConstant/d[i][j])/d[i][j];
                    if (!Double.isFinite(l) || l <= 0 || !Double.isFinite(k) || k <= 0)
                        throw new IllegalArgumentException("Kamada-Kawai spring scale overflow/underflow");
                    lengths[i][j] = l; springs[i][j] = k;
                }
            }
        }
        length = lengths; spring = springs;
    }

    @Override protected void advance()
    {
        if (length == null) prepare();
        double[] selectedGradient = selected < 0 ? null : derivatives(selected);
        if (selectedGradient == null || Math.hypot(selectedGradient[0], selectedGradient[1]) <= config.tolerance)
        {
            selected = -1; double greatest = config.tolerance;
            for (int i = 0; i < x.length; i++) if (!pins[i])
            {
                checkpoint(); double[] g = derivatives(i); double norm = Math.hypot(g[0], g[1]);
                if (norm > greatest) { greatest = norm; selected = i; }
            }
        }
        if (selected < 0) { status = LayoutDiagnostics.Termination.CONVERGED; return; }
        double[] g = derivatives(selected);
        double determinant = g[2]*g[3]-g[4]*g[4];
        double dx = (g[4]*g[1]-g[3]*g[0])/determinant;
        double dy = (g[4]*g[0]-g[2]*g[1])/determinant;
        if (!Double.isFinite(dx) || !Double.isFinite(dy) || g[0]*dx+g[1]*dy >= 0)
        { dx = -g[0]/g[5]; dy = -g[1]/g[5]; }
        double before = localEnergy(selected, x[selected], y[selected]);
        double fraction = 1, nx = x[selected], ny = y[selected];
        boolean accepted = false;
        for (int attempt = 0; attempt < 60; attempt++)
        {
            checkpoint();
            nx = x[selected]+fraction*dx; ny = y[selected]+fraction*dy;
            if (Double.isFinite(nx) && Double.isFinite(ny) && localEnergy(selected, nx, ny) < before)
            { accepted = true; break; }
            fraction /= 2;
        }
        if (!accepted)
        {
            // A high residual is not convergence. Expose numerical stagnation as
            // a limit, retaining the last valid positions for the caller.
            status = LayoutDiagnostics.Termination.LIMIT_REACHED; return;
        }
        double[] nextX = x.clone(), nextY = y.clone();
        nextX[selected] = nx; nextY[selected] = ny;
        commit(nextX, nextY);
    }

    private double[] derivatives(int i)
    {
        double gx = 0, gy = 0, hxx = 0, hyy = 0, hxy = 0, sum = 0;
        for (int j = 0; j < x.length; j++) if (spring[i][j] > 0)
        {
            checkpoint();
            double dx = x[i]-x[j], dy = y[i]-y[j], r = Math.hypot(dx, dy);
            if (r == 0)
            {
                // The analytic derivative is undefined at coincidence. Pick a
                // deterministic antisymmetric direction for this exceptional case.
                double angle = ((Math.min(i,j)*73856093L ^ Math.max(i,j)*19349663L)&0xffff)*2*Math.PI/65536;
                r = config.drawingSize*1e-9;
                dx = Math.cos(angle)*r*(i < j ? 1 : -1); dy = Math.sin(angle)*r*(i < j ? 1 : -1);
            }
            double k = spring[i][j], l = length[i][j], ux = dx/r, uy = dy/r;
            gx += k*(dx-l*ux); gy += k*(dy-l*uy);
            hxx += k*(1-(l/r)*uy*uy); hyy += k*(1-(l/r)*ux*ux); hxy += k*(l/r)*ux*uy;
            sum += k;
        }
        if (!Double.isFinite(gx) || !Double.isFinite(gy) || !Double.isFinite(hxx)
            || !Double.isFinite(hyy) || !Double.isFinite(hxy) || !Double.isFinite(sum))
            throw new IllegalArgumentException("Kamada-Kawai derivative overflow; rescale input coordinates or weights");
        return new double[]{gx, gy, hxx, hyy, hxy, sum};
    }
    private double localEnergy(int i, double xx, double yy)
    {
        double result = 0;
        for (int j = 0; j < x.length; j++) if (spring[i][j] > 0)
        {
            checkpoint(); double error = Math.hypot(xx-x[j], yy-y[j])-length[i][j];
            result += spring[i][j]*error*error/2;
        }
        if (!Double.isFinite(result)) throw new IllegalArgumentException("Kamada-Kawai energy overflow");
        return result;
    }
}
