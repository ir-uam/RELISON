/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;

/** 3D FR settings. Exact repulsion; no 2D quadtree approximation is reused. */
public final class FruchtermanReingold3DConfig {
    public final boolean bounded, weighted;
    public final double width,height,depth,initialTemperature,idealLength,cooling,tolerance;
    /** 600-unit cube, temperature 60, linear cooling. */
    public FruchtermanReingold3DConfig() { this(600,600,600,60,.001,false); }
    /**
     * @param width frame width
     * @param height frame height
     * @param depth frame depth
     * @param initialTemperature initial displacement cap
     * @param tolerance stopping threshold
     * @param weighted weighted attraction
     */
    public FruchtermanReingold3DConfig(double width,double height,double depth,double initialTemperature,double tolerance,boolean weighted) {
        for(double v:new double[]{width,height,depth,initialTemperature})
            if(!Double.isFinite(v)||v<=0) throw new IllegalArgumentException("3D frame and temperature must be positive and finite");
        if(!Double.isFinite(tolerance)||tolerance<0 || !Double.isFinite(width*height*depth) || width*height*depth==0)
            throw new IllegalArgumentException("Invalid 3D FR settings");
        this.width=width;this.height=height;this.depth=depth;this.initialTemperature=initialTemperature;
        this.tolerance=tolerance;this.weighted=weighted;bounded=true;idealLength=0;cooling=0;
    }
    /** Unbounded extension with geometric cooling.
     * @param idealLength edge scale
     * @param cooling temperature multiplier in (0,1)
     * @param tolerance stopping threshold
     * @param weighted weighted attraction
     */
    public FruchtermanReingold3DConfig(double idealLength,double cooling,double tolerance,boolean weighted) {
        if(!Double.isFinite(idealLength)||idealLength<=0||!Double.isFinite(cooling)||cooling<=0||cooling>=1
            ||!Double.isFinite(tolerance)||tolerance<0) throw new IllegalArgumentException("Invalid 3D FR settings");
        this.idealLength=idealLength;this.cooling=cooling;this.tolerance=tolerance;this.weighted=weighted;
        bounded=false;width=height=depth=0;initialTemperature=idealLength;
    }
    /**
     * @param nodes node count
     * @return ideal edge length, cbrt(volume/nodeCount) in bounded mode
     */
    public double edgeScale(long nodes) { return bounded ? Math.cbrt(width*height*depth/Math.max(1,nodes)) : idealLength; }
}
