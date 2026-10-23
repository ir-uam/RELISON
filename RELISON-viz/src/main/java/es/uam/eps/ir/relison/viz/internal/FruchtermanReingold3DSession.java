/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.internal;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.layouts.FruchtermanReingold3DConfig;
import java.util.*;

/**
 * Independent, caller-stepped 3D FR simulation.
 * @param <U> node type
 */
public final class FruchtermanReingold3DSession<U> extends NumericalLayoutSession<U> {
    private final FruchtermanReingold3DConfig config;
    private final double k;
    private final List<int[]> edges = new ArrayList<>();
    private final List<Double> weights = new ArrayList<>();
    private int iteration,stable;
    private double temperature;
    /**
     * @param input graph
     * @param request constraints
     * @param config settings
     */
    public FruchtermanReingold3DSession(Graph<U> input,LayoutRequest<U> request,FruchtermanReingold3DConfig config) {
        super(input,request,"fruchterman-reingold-3d",config.edgeScale(input.getVertexCount()),true);
        this.config=config;k=config.edgeScale(x.length);temperature=config.initialTemperature;
        if(!Double.isFinite(k)||k<=0) throw new IllegalArgumentException("Invalid FR scale");
        Random placement = new Random(request.getSeed());
        for(int i=0;i<x.length;i++) {
            if(config.bounded) {
                if(!request.getInitialPositions().containsKey(graph.getNodes().get(i))) {
                    x[i]=(placement.nextDouble()-.5)*config.width;
                    y[i]=(placement.nextDouble()-.5)*config.height;
                    z[i]=(placement.nextDouble()-.5)*config.depth;
                }
                if(pins[i]&&(Math.abs(x[i])>config.width/2||Math.abs(y[i])>config.height/2||Math.abs(z[i])>config.depth/2))
                    throw new IllegalArgumentException("Pinned node is outside the 3D FR frame");
                x[i]=clamp(x[i],config.width);y[i]=clamp(y[i],config.height);z[i]=clamp(z[i],config.depth);
            }
            int[] targets=graph.getTargets(i);double[] w=graph.getWeights(i);
            for(int j=0;j<targets.length;j++) {
                if(targets[j]==i||(!graph.isDirected()&&targets[j]<i)) continue;
                double weight=config.weighted?w[j]:1;
                if(!Double.isFinite(weight)||weight<0) throw new IllegalArgumentException("FR attraction requires nonnegative finite weights");
                edges.add(new int[]{i,targets[j]});weights.add(weight);
            }
        }
    }
    private static double clamp(double value,double side) { return Math.max(-side/2,Math.min(side/2,value)); }
    @Override protected void advance() {
        int n=x.length;
        double[] fx=new double[n],fy=new double[n],fz=new double[n];
        for(int i=0;i<n;i++) {
            checkpoint();
            for(int j=i+1;j<n;j++) {
                if((j&255)==0) checkpoint();
                double dx=x[i]-x[j],dy=y[i]-y[j],dz=z[i]-z[j],d=Math.hypot(Math.hypot(dx,dy),dz);
                if(d==0) {
                    dx=(random.nextDouble()-.5)*1e-6;dy=(random.nextDouble()-.5)*1e-6;dz=(random.nextDouble()-.5)*1e-6;
                    d=Math.max(1e-12,Math.hypot(Math.hypot(dx,dy),dz));
                }
                double force=(k/d)*(k/d);
                fx[i]+=dx*force;fy[i]+=dy*force;fz[i]+=dz*force;
                fx[j]-=dx*force;fy[j]-=dy*force;fz[j]-=dz*force;
            }
        }
        for(int e=0;e<edges.size();e++) {
            checkpoint();int i=edges.get(e)[0],j=edges.get(e)[1];
            double dx=x[i]-x[j],dy=y[i]-y[j],dz=z[i]-z[j];
            double force=Math.hypot(Math.hypot(dx,dy),dz)/k*weights.get(e);
            fx[i]-=dx*force;fy[i]-=dy*force;fz[i]-=dz*force;
            fx[j]+=dx*force;fy[j]+=dy*force;fz[j]+=dz*force;
        }
        double[] nx=x.clone(),ny=y.clone(),nz=z.clone();double maximum=0;
        for(int i=0;i<n;i++) if(!pins[i]) {
            double length=Math.hypot(Math.hypot(fx[i],fy[i]),fz[i]);
            if(!Double.isFinite(length)) throw new IllegalArgumentException("3D force overflow");
            double factor=length==0?0:Math.min(temperature,length)/length;
            nx[i]+=fx[i]*factor;ny[i]+=fy[i]*factor;nz[i]+=fz[i]*factor;
            if(config.bounded) { nx[i]=clamp(nx[i],config.width);ny[i]=clamp(ny[i],config.height);nz[i]=clamp(nz[i],config.depth); }
            maximum=Math.max(maximum,Math.hypot(Math.hypot(nx[i]-x[i],ny[i]-y[i]),nz[i]-z[i]));
        }
        commit(nx,ny,nz);iteration++;
        temperature=config.bounded?config.initialTemperature*Math.max(0,1-(double)iteration/Math.max(1,request.getMaxIterations())):temperature*config.cooling;
        stable=maximum<=config.tolerance?stable+1:0;
        if(stable>=3) status=LayoutDiagnostics.Termination.CONVERGED;
    }
}
