/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;
import es.uam.eps.ir.relison.viz.internal.AbstractStaticLayout;
/** Immutable full-stress majorization settings. */
public final class StressMajorizationConfig
{
    public final double edgeLength, tolerance;
    public final boolean weighted;
    /** Unit-distance scale 50, relative stress tolerance 0.0001, unweighted paths. */
    public StressMajorizationConfig() { this(50, 0.0001, false); }
    /**
     * @param edgeLength drawing length of one graph-distance unit
     * @param tolerance nonnegative relative stress-decrease threshold
     * @param weighted whether edge weights represent positive path lengths
     */
    public StressMajorizationConfig(double edgeLength, double tolerance, boolean weighted)
    {
        this.edgeLength = AbstractStaticLayout.positive(edgeLength, "edgeLength");
        if (!Double.isFinite(tolerance) || tolerance < 0) throw new IllegalArgumentException("Invalid stress tolerance");
        this.tolerance = tolerance; this.weighted = weighted;
    }
}
