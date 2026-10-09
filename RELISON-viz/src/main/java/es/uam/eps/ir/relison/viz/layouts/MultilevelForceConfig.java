/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;

/** Settings for Hu's edge-collapsing multilevel spring-electrical model. */
public final class MultilevelForceConfig
{
    /** Initialization scale, opening angle (zero: exact), repulsion C, cooling t and movement tolerance. */
    public final double initialScale, theta, repulsion, cooling, tolerance;
    /** Maximum sweeps at each level, in addition to the request's total sweep budget. */
    public final int levelIterations;
    /** Whether original edge weights are positive attraction strengths. */
    public final boolean weighted;
    /** Defaults: C=.2, t=.9, opening angle .8, tolerance .001, 100 sweeps per level. */
    public MultilevelForceConfig() { this(50, .8, .2, .9, .001, 100, false); }
    /**
     * @param initialScale initialization scale
     * @param theta Barnes-Hut opening angle; zero uses exact forces
     * @param repulsion electrical force multiplier C
     * @param cooling cooling multiplier in (0,1)
     * @param tolerance movement tolerance relative to K
     * @param levelIterations maximum sweeps per level
     * @param weighted use edge weights as attraction strengths
     */
    public MultilevelForceConfig(double initialScale, double theta, double repulsion, double cooling,
                                double tolerance, int levelIterations, boolean weighted)
    {
        if (!Double.isFinite(initialScale) || initialScale <= 0 || !Double.isFinite(theta) || theta < 0
            || !Double.isFinite(repulsion) || repulsion <= 0 || !Double.isFinite(cooling) || cooling <= 0 || cooling >= 1
            || !Double.isFinite(tolerance) || tolerance <= 0 || levelIterations <= 0)
            throw new IllegalArgumentException("Invalid multilevel force settings");
        this.initialScale=initialScale; this.theta=theta; this.repulsion=repulsion; this.cooling=cooling;
        this.tolerance=tolerance; this.levelIterations=levelIterations; this.weighted=weighted;
    }
}
