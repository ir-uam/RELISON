/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.transforms;
import es.uam.eps.ir.relison.viz.*;
import java.util.*;

/** Iterative sphere separation in layout units, preserving spatial pins.
 * Resolves physical collisions, not projected screen occlusion. O(V^2) per pass.
 * @param <U> node type */
public final class OverlapRemoval3D<U> implements LayoutPostProcessor<U> {
    private final double gap;
    private final int passes;
    /** Unit gap, at most 200 passes. */
    public OverlapRemoval3D() { this(1,200); }
    /**
     * @param gap minimum surface gap
     * @param passes maximum passes
     */
    public OverlapRemoval3D(double gap,int passes) {
        if(!Double.isFinite(gap)||gap<0||passes<1) throw new IllegalArgumentException("Invalid 3D overlap settings");
        this.gap=gap;this.passes=passes;
    }
    @Override public LayoutResult<U> process(LayoutResult<U> result,LayoutRequest<U> request) {
        if(result.getSpaceDimensions()!=3) throw new IllegalArgumentException("3D overlap removal requires a spatial result");
        List<U> nodes=new ArrayList<>(result.getPositions3D().keySet());
        if(!new HashSet<>(nodes).containsAll(request.getNodeSizes().keySet())||!new HashSet<>(nodes).containsAll(request.getPinnedNodes()))
            throw new IllegalArgumentException("Overlap constraints refer to absent nodes");
        int n=nodes.size();double[] x=new double[n],y=new double[n],z=new double[n],r=new double[n];boolean[] pin=new boolean[n];
        for(int i=0;i<n;i++) {
            Point3D p=result.getPositions3D().get(nodes.get(i));x[i]=p.getX();y[i]=p.getY();z[i]=p.getZ();
            r[i]=request.getNodeSizes().getOrDefault(nodes.get(i),0.0);pin[i]=request.getPinnedNodes().contains(nodes.get(i));
            if(pin[i]&&!p.equals(request.getInitialPositions3D().get(nodes.get(i)))) throw new IllegalArgumentException("Spatial pin was moved");
        }
        Random random=new Random(request.getSeed());
        for(int pass=0;pass<passes;pass++) {
            boolean moved=false;
            for(int i=0;i<n;i++) for(int j=i+1;j<n;j++) {
                if(Thread.currentThread().isInterrupted()||request.getCancellation().getAsBoolean())
                    return output(nodes,x,y,z,new LayoutDiagnostics(result.getDiagnostics().getAlgorithmId(),LayoutDiagnostics.Termination.CANCELLED,
                        result.getDiagnostics().getIterations(),result.getDiagnostics().getMaximumDisplacement()));
                double dx=x[j]-x[i],dy=y[j]-y[i],dz=z[j]-z[i],distance=Math.hypot(Math.hypot(dx,dy),dz),required=r[i]+r[j]+gap;
                if(!Double.isFinite(required)||!Double.isFinite(distance)) throw new IllegalArgumentException("3D overlap distance overflow");
                if(distance+1e-8>=required) continue;
                if(pin[i]&&pin[j]) throw new IllegalArgumentException("Overlapping pinned spheres cannot be separated");
                if(distance<1e-12) {
                    double latitude=2*random.nextDouble()-1,angle=random.nextDouble()*2*Math.PI;
                    double radial=Math.sqrt(1-latitude*latitude);dx=radial*Math.cos(angle);dy=latitude;dz=radial*Math.sin(angle);
                } else { dx/=distance;dy/=distance;dz/=distance; }
                double push=required-distance+1e-8,a=pin[i]?0:(pin[j]?1:.5),b=pin[j]?0:(pin[i]?1:.5);
                x[i]-=dx*push*a;y[i]-=dy*push*a;z[i]-=dz*push*a;
                x[j]+=dx*push*b;y[j]+=dy*push*b;z[j]+=dz*push*b;moved=true;
            }
            if(!moved) return output(nodes,x,y,z,result.getDiagnostics());
        }
        for(int i=0;i<n;i++) for(int j=i+1;j<n;j++)
            if(Math.hypot(Math.hypot(x[i]-x[j],y[i]-y[j]),z[i]-z[j])+1e-8<r[i]+r[j]+gap)
                throw new IllegalArgumentException("3D overlap removal reached its pass limit");
        return output(nodes,x,y,z,result.getDiagnostics());
    }
    private LayoutResult<U> output(List<U> nodes,double[] x,double[] y,double[] z,LayoutDiagnostics diagnostics) {
        Map<U,Point3D> positions=new LinkedHashMap<>();
        for(int i=0;i<nodes.size();i++) positions.put(nodes.get(i),new Point3D(x[i],y[i],z[i]));
        return LayoutResult.spatial(positions,diagnostics);
    }
}
