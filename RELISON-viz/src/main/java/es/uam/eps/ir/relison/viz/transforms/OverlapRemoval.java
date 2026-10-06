/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.transforms;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
import java.util.*;

/**
 * Iterative circular-glyph separation, preserving exact pins. Radii are taken
 * from the request; missing radii are zero. Unresolved constraints raise an error.
 * Pair testing is quadratic per pass.
 * @param <U> node type
 */
public final class OverlapRemoval<U> implements LayoutPostProcessor<U> {
    private final double gap;
    private final int passes;
    /** Unit gap and at most 200 passes. */
    public OverlapRemoval() { this(1,200); }
    /**
     * @param gap non-negative minimum gap between glyph boundaries
     * @param passes positive maximum separation passes
     */
    public OverlapRemoval(double gap,int passes) {
        if(!Double.isFinite(gap)||gap<0||passes<1) throw new IllegalArgumentException("Invalid overlap-removal settings");
        this.gap=gap; this.passes=passes;
    }
    @Override
    public LayoutResult<U> process(LayoutResult<U> result,LayoutRequest<U> request) {
        List<U> nodes=new ArrayList<>(result.getPositions().keySet());
        if(!new HashSet<>(nodes).containsAll(request.getNodeSizes().keySet())
            || !new HashSet<>(nodes).containsAll(request.getPinnedNodes()))
            throw new IllegalArgumentException("Size or pin constraints refer to absent nodes");
        int n=nodes.size();
        double[] x=new double[n],y=new double[n],r=new double[n]; boolean[] pins=new boolean[n];
        for(int i=0;i<n;i++) {
            U node=nodes.get(i); Pair<Double> p=result.getPositions().get(node);
            pins[i]=request.getPinnedNodes().contains(node);
            if(pins[i]&&!p.equals(request.getInitialPositions().get(node)))
                throw new IllegalArgumentException("Input layout moved a pinned node");
            x[i]=p.v1(); y[i]=p.v2(); r[i]=request.getNodeSizes().getOrDefault(node,0.0);
        }
        for(int pass=0;pass<passes;pass++) {
            boolean moved=false;
            for(int i=0;i<n;i++) for(int j=i+1;j<n;j++) {
                if(Thread.currentThread().isInterrupted()||request.getCancellation().getAsBoolean())
                    return output(nodes,x,y,new LayoutDiagnostics(result.getDiagnostics().getAlgorithmId(),
                        LayoutDiagnostics.Termination.CANCELLED,result.getDiagnostics().getIterations(),result.getDiagnostics().getMaximumDisplacement()));
                double vx=x[j]-x[i],vy=y[j]-y[i],distance=Math.hypot(vx,vy),required=r[i]+r[j]+gap;
                if(!Double.isFinite(required)) throw new IllegalArgumentException("Node-size sum overflow");
                if(distance+1e-8>=required) continue;
                if(pins[i]&&pins[j]) throw new IllegalArgumentException("Overlapping pinned nodes cannot be separated");
                if(distance<1e-12) {
                    double angle=((i*73856093L ^ j*19349663L)&0xffff)*2*Math.PI/65536;
                    vx=Math.cos(angle); vy=Math.sin(angle);
                } else { vx/=distance; vy/=distance; }
                double push=required-distance+1e-8;
                double a=pins[i]?0:(pins[j]?1:0.5), c=pins[j]?0:(pins[i]?1:0.5);
                x[i]-=vx*push*a; y[i]-=vy*push*a; x[j]+=vx*push*c; y[j]+=vy*push*c;
                moved=true;
            }
            if(!moved) return output(nodes,x,y,result.getDiagnostics());
        }
        for(int i=0;i<n;i++) for(int j=i+1;j<n;j++)
            if(Math.hypot(x[j]-x[i],y[j]-y[i])+1e-8<r[i]+r[j]+gap)
                throw new IllegalArgumentException("Overlap removal reached its pass limit with unresolved collisions");
        return output(nodes,x,y,result.getDiagnostics());
    }
    private LayoutResult<U> output(List<U> nodes,double[] x,double[] y,LayoutDiagnostics diagnostics) {
        Map<U,Pair<Double>> positions=new LinkedHashMap<>();
        for(int i=0;i<nodes.size();i++) positions.put(nodes.get(i),new Pair<>(x[i],y[i]));
        return new LayoutResult<>(positions,diagnostics);
    }
}
