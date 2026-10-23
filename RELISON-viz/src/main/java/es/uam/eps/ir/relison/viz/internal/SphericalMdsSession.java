/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.internal;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.layouts.SphericalMdsConfig;
import java.util.*;

/** Caller-stepped spherical angular SGD; each iteration visits every pair once.
 * @param <U> node type */
public final class SphericalMdsSession<U> extends NumericalLayoutSession<U> {
    private final SphericalMdsConfig config;
    private final double[] latitude,longitude;
    private final double[][] distances;
    private final int[] pairI,pairJ,order;
    private double etaMax,etaSwitch,decay,previousStress;
    private int epoch,switchEpoch=-1;
    private boolean initialized;
    /**
     * Initializes angular positions and exact shortest-path targets.
     * @param input graph
     * @param request scheduling constraints
     * @param config model settings
     */
    public SphericalMdsSession(Graph<U> input,LayoutRequest<U> request,SphericalMdsConfig config) {
        super(input,request,"spherical-mds-3d",config.radius,true); this.config=config;
        int n=x.length; latitude=new double[n];longitude=new double[n];
        for(int i=0;i<n;i++) {
            U node=graph.getNodes().get(i); Point3D p=request.getInitialPositions3D().get(node);
            if(p!=null) {
                double norm=Math.hypot(Math.hypot(p.getX(),p.getY()),p.getZ());
                if(norm==0) throw new IllegalArgumentException("Spherical warm starts must be nonzero");
                if(pins[i]&&Math.abs(norm-config.radius)>config.radius*1e-10) throw new IllegalArgumentException("Spherical pins must lie on the requested sphere");
                latitude[i]=Math.asin(Math.max(-1,Math.min(1,p.getZ()/norm)));longitude[i]=Math.atan2(p.getY(),p.getX());
            } else { latitude[i]=Math.asin(2*random.nextDouble()-1);longitude[i]=2*Math.PI*random.nextDouble(); }
            if(!pins[i]) { x[i]=config.radius*Math.cos(latitude[i])*Math.cos(longitude[i]);y[i]=config.radius*Math.cos(latitude[i])*Math.sin(longitude[i]);z[i]=config.radius*Math.sin(latitude[i]); }
        }
        if(status==LayoutDiagnostics.Termination.CANCELLED) {
            distances=new double[0][];pairI=pairJ=order=new int[0];return;
        }
        distances=GraphDistances.compute(graph,config.weighted,()->{
            if(Thread.currentThread().isInterrupted()||request.getCancellation().getAsBoolean()) throw new IllegalArgumentException("Spherical MDS cancelled during distance preparation");
        });
        double diameter=0; for(int i=0;i<n;i++) for(int j=0;j<i;j++) {
            if(!Double.isFinite(distances[i][j])) throw new IllegalArgumentException("Spherical MDS requires a connected graph (direction is ignored)");
            diameter=Math.max(diameter,distances[i][j]);
        }
        long pairs=(long)n*(n-1)/2; if(pairs>Integer.MAX_VALUE) throw new IllegalArgumentException("Too many spherical distance pairs");
        pairI=new int[(int)pairs];pairJ=new int[(int)pairs];order=new int[(int)pairs];
        double minimum=Double.POSITIVE_INFINITY,maximum=0; int k=0;
        for(int i=0;i<n;i++) for(int j=0;j<i;j++) {
            double d=(distances[i][j]/diameter)*Math.PI; distances[i][j]=distances[j][i]=d;
            if(!(d>0)||!Double.isFinite(d)) throw new IllegalArgumentException("Spherical target distance underflow");
            minimum=Math.min(minimum,d);maximum=Math.max(maximum,d);
            pairI[k]=i;pairJ[k]=j;order[k]=k;k++;
        }
        if(k>0) { etaMax=maximum*maximum;etaSwitch=minimum*minimum;decay=Math.log(etaMax/(.01*etaSwitch))/29; }
        previousStress=stress(latitude,longitude);
        initialized=true;
        if(k==0&&status==LayoutDiagnostics.Termination.RUNNING) status=LayoutDiagnostics.Termination.CONVERGED;
    }
    @Override protected void advance() {
        double[] a=latitude.clone(),b=longitude.clone();
        for(int k=order.length-1;k>0;k--) { int j=random.nextInt(k+1),v=order[k];order[k]=order[j];order[j]=v; }
        double eta=etaMax*Math.exp(-decay*epoch);
        if(eta<etaSwitch&&switchEpoch<0) switchEpoch=epoch;
        if(switchEpoch>=0) eta=etaSwitch/(1+decay*(epoch-switchEpoch));
        eta=Math.min(config.learningRate,eta);
        for(int pair:order) {
            checkpoint(); int i=pairI[pair],j=pairJ[pair];
            if(pins[i]&&pins[j]) continue;
            double[] gradient=pairGradient(a[i],b[i],a[j],b[j],distances[i][j]);
            if(!pins[i]) { a[i]-=eta*gradient[0]; b[i]-=eta*gradient[1]; }
            if(!pins[j]) { a[j]-=eta*gradient[2]; b[j]-=eta*gradient[3]; }
        }
        double value=stress(a,b);
        double[] nx=x.clone(),ny=y.clone(),nz=z.clone();
        for(int i=0;i<x.length;i++) if(!pins[i]) {
            nx[i]=config.radius*Math.cos(a[i])*Math.cos(b[i]);ny[i]=config.radius*Math.cos(a[i])*Math.sin(b[i]);nz[i]=config.radius*Math.sin(a[i]);
        }
        commit(nx,ny,nz);System.arraycopy(a,0,latitude,0,a.length);System.arraycopy(b,0,longitude,0,b.length);epoch++;
        if(epoch>=16&&Math.abs(value-previousStress)<=config.tolerance) status=LayoutDiagnostics.Termination.CONVERGED;
        previousStress=value;
    }
    /** Angular derivatives of one unweighted squared geodesic residual (Eq. 4).
     * @param a first latitude
     * @param b first longitude
     * @param c second latitude
     * @param d second longitude
     * @param target target angle
     * @return derivatives in a,b,c,d order */
    public static double[] pairGradient(double a,double b,double c,double d,double target) {
        double sa=Math.sin(a),ca=Math.cos(a),sc=Math.sin(c),cc=Math.cos(c),delta=b-d;
        double cosine=Math.max(-1,Math.min(1,sa*sc+ca*cc*Math.cos(delta)));
        double angle=Math.acos(cosine),denominator=Math.sqrt(Math.max(0,1-cosine*cosine));
        if(denominator<1e-12) {
            if(Math.abs(angle-target)<1e-12) return new double[4];
            throw new IllegalArgumentException("Spherical gradient is undefined for coincident/antipodal positions; choose a different seed or warm start");
        }
        double factor=2*(angle-target)/denominator;
        double lon=ca*cc*Math.sin(delta);
        return new double[]{factor*(-ca*sc+sa*cc*Math.cos(delta)),factor*lon,
            factor*(-cc*sa+sc*ca*Math.cos(delta)),-factor*lon};
    }
    private double stress(double[] a,double[] b) {
        double sum=0; for(int k=0;k<pairI.length;k++) {
            if((k&1023)==0) {
                if(initialized) checkpoint();
                else if(Thread.currentThread().isInterrupted()||request.getCancellation().getAsBoolean()) throw new IllegalArgumentException("Spherical stress evaluation cancelled");
            }
            int i=pairI[k],j=pairJ[k];double dot=Math.sin(a[i])*Math.sin(a[j])+Math.cos(a[i])*Math.cos(a[j])*Math.cos(b[i]-b[j]);
            double residual=Math.acos(Math.max(-1,Math.min(1,dot)))-distances[i][j]; sum+=residual*residual;
        }
        return sum/Math.max(1,(double)x.length*x.length);
    }
}
