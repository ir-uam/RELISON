/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;

/** Settings for Miller, Huroyan and Kobourov's spherical MDS (2022). */
public final class SphericalMdsConfig {
    /** Display sphere radius; optimization is on the unit sphere. */
    public final double radius;
    /** Upper bound on the piecewise learning-rate schedule. */
    public final double learningRate;
    /** Absolute change in mean stress required for convergence. */
    public final double tolerance;
    /** Whether positive input edge weights are shortest-path lengths. */
    public final boolean weighted;
    /** Paper's recommended learning-rate cap and convergence threshold. */
    public SphericalMdsConfig() { this(100,.1,1e-7,false); }
    /**
     * Configures spherical SGD and its display scale.
     * @param radius display radius
     * @param learningRate positive learning-rate cap, at most 0.1
     * @param tolerance nonnegative stress-change threshold
     * @param weighted whether edge weights represent positive path lengths
     */
    public SphericalMdsConfig(double radius,double learningRate,double tolerance,boolean weighted) {
        if(!Double.isFinite(radius)||radius<=0||!Double.isFinite(learningRate)||learningRate<=0||learningRate>.1
            ||!Double.isFinite(tolerance)||tolerance<0) throw new IllegalArgumentException("Invalid spherical MDS settings");
        this.radius=radius;this.learningRate=learningRate;this.tolerance=tolerance;this.weighted=weighted;
    }
}
