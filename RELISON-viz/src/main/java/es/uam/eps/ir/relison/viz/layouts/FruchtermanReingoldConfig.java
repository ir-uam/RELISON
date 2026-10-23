/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;
/** Immutable Fruchterman-Reingold force settings. */
public final class FruchtermanReingoldConfig {
    public final double idealLength, cooling, theta, tolerance;
    public final boolean weighted;
    /** Whether to derive the edge scale from a bounded drawing frame. */
    public final boolean bounded;
    /** Frame dimensions and initial temperature for reference mode. */
    public final double width, height, initialTemperature;
    /** Reference mode: 600 by 600 frame, temperature 60, linear cooling, exact repulsion. */
    public FruchtermanReingoldConfig() { this(600, 600, 60, 0, 0.001, false); }
    /**
     * Configures the unbounded extension with geometric cooling.
     * @param idealLength positive edge scale
     * @param cooling temperature multiplier strictly between zero and one
     * @param theta Barnes-Hut opening ratio in [0,2]; zero is exact
     * @param tolerance non-negative displacement tolerance
     * @param weighted whether weights scale attraction
     */
    public FruchtermanReingoldConfig(double idealLength, double cooling, double theta, double tolerance, boolean weighted) {
        if (!Double.isFinite(idealLength) || idealLength <= 0 || !Double.isFinite(cooling) || cooling <= 0 || cooling >= 1
            || !Double.isFinite(theta) || theta < 0 || theta > 2 || !Double.isFinite(tolerance) || tolerance < 0)
            throw new IllegalArgumentException("Invalid Fruchterman-Reingold settings");
        this.bounded=false; this.width=0; this.height=0; this.initialTemperature=idealLength;
        this.idealLength=idealLength; this.cooling=cooling; this.theta=theta; this.tolerance=tolerance; this.weighted=weighted;
    }
    /**
     * Configures the reference drawing frame: k = sqrt(width * height / nodeCount).
     * Temperature decreases linearly to zero over the requested iteration budget.
     * @param width positive frame width
     * @param height positive frame height
     * @param initialTemperature positive initial maximum displacement
     * @param theta Barnes-Hut opening ratio in [0,2]; zero selects exact forces
     * @param tolerance non-negative stopping tolerance
     * @param weighted whether weights scale attraction (an extension)
     */
    public FruchtermanReingoldConfig(double width,double height,double initialTemperature,
            double theta,double tolerance,boolean weighted) {
        for(double value:new double[]{width,height,initialTemperature,theta,tolerance})
            if(!Double.isFinite(value)||value<0) throw new IllegalArgumentException("Invalid Fruchterman-Reingold settings");
        if(width==0||height==0||initialTemperature==0||theta>2||!Double.isFinite(width*height)||width*height==0)
            throw new IllegalArgumentException("Invalid Fruchterman-Reingold frame");
        this.bounded=true; this.width=width; this.height=height; this.initialTemperature=initialTemperature;
        this.idealLength=0; this.cooling=0; this.theta=theta; this.tolerance=tolerance; this.weighted=weighted;
    }
}
