/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;
import es.uam.eps.ir.relison.viz.internal.AbstractStaticLayout;
/** Immutable Kamada-Kawai spring-energy settings. */
public final class KamadaKawaiConfig
{
    public final double drawingSize, springConstant, tolerance;
    public final boolean weighted;
    /** Drawing size 600, spring constant 1, gradient tolerance 0.0001. */
    public KamadaKawaiConfig() { this(600, 1, 0.0001, false); }
    /**
     * @param drawingSize desired component graph-diameter length
     * @param springConstant K in k_ij = K / d_ij squared
     * @param tolerance nonnegative gradient-norm stopping threshold
     * @param weighted whether edge weights are positive path lengths
     */
    public KamadaKawaiConfig(double drawingSize, double springConstant, double tolerance, boolean weighted)
    {
        this.drawingSize = AbstractStaticLayout.positive(drawingSize, "drawingSize");
        this.springConstant = AbstractStaticLayout.positive(springConstant, "springConstant");
        if (!Double.isFinite(tolerance) || tolerance < 0) throw new IllegalArgumentException("Invalid gradient tolerance");
        this.tolerance = tolerance; this.weighted = weighted;
    }
}
