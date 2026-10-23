/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;
/** Immutable ForceAtlas2 settings. Parallel multiplicity contributes to degree masses. */
public final class ForceAtlas2Config {
    public final double scaling, gravity, jitterTolerance, theta, weightInfluence, tolerance;
    public final boolean linLog, strongGravity;
    /** Optional attraction distribution, collision forces and weight transforms. */
    public final boolean outboundAttractionDistribution, adjustSizes, normalizeWeights, invertWeights;
    /** Scaling 100, gravity 1, jitter 1, exact repulsion, linear attraction, unit weights. */
    public ForceAtlas2Config() { this(100, 1, 1, 0, 0, 0.001, false, false); }
    /**
     * @param scaling positive repulsion coefficient
     * @param gravity non-negative origin attraction
     * @param jitterTolerance positive speed tolerance
     * @param theta Barnes-Hut opening ratio in [0,2]; zero is exact
     * @param weightInfluence non-negative edge-weight exponent; zero ignores weights
     * @param tolerance non-negative displacement tolerance
     * @param linLog logarithmic edge attraction
     * @param strongGravity distance-proportional gravity
     */
    public ForceAtlas2Config(double scaling, double gravity, double jitterTolerance, double theta,
        double weightInfluence, double tolerance, boolean linLog, boolean strongGravity) {
        this(scaling,gravity,jitterTolerance,theta,weightInfluence,tolerance,linLog,strongGravity,
            false,false,false,false);
    }
    /**
     * Full ForceAtlas2 settings. Size adjustment uses exact repulsion.
     * @param scaling positive repulsion coefficient
     * @param gravity non-negative origin attraction
     * @param jitterTolerance positive speed tolerance
     * @param theta Barnes-Hut opening ratio; zero selects exact pairs
     * @param weightInfluence non-negative weight exponent
     * @param tolerance stopping displacement tolerance
     * @param linLog logarithmic attraction
     * @param strongGravity distance-proportional gravity
     * @param outboundAttractionDistribution divide attraction by source mass, compensated by mean mass
     * @param adjustSizes use request radii in collision forces and the reference collision movement limiter
     * @param normalizeWeights normalize transformed weights to [0,1] before exponentiation
     * @param invertWeights invert nonzero weights before normalization and exponentiation
     */
    public ForceAtlas2Config(double scaling,double gravity,double jitterTolerance,double theta,
            double weightInfluence,double tolerance,boolean linLog,boolean strongGravity,
            boolean outboundAttractionDistribution,boolean adjustSizes,boolean normalizeWeights,boolean invertWeights) {
        this.outboundAttractionDistribution=outboundAttractionDistribution; this.adjustSizes=adjustSizes;
        this.normalizeWeights=normalizeWeights; this.invertWeights=invertWeights;
        for(double value:new double[]{scaling,gravity,jitterTolerance,theta,weightInfluence,tolerance})
            if(!Double.isFinite(value)||value<0) throw new IllegalArgumentException("Invalid ForceAtlas2 settings");
        if(scaling==0||jitterTolerance==0||theta>2) throw new IllegalArgumentException("Invalid ForceAtlas2 settings");
        this.scaling=scaling; this.gravity=gravity; this.jitterTolerance=jitterTolerance; this.theta=theta;
        this.weightInfluence=weightInfluence; this.tolerance=tolerance; this.linLog=linLog; this.strongGravity=strongGravity;
    }
}
