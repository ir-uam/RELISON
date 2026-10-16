/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;

import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.*;
import java.util.*;

/** Filled spherical node clusters, optionally arranged around a spherical group envelope.
 * This is the 3D counterpart of the GUI's filled-disc circle packing.
 * @param <U> node type */
public final class SpherePacking3DLayout<U> extends AbstractStatic3DLayout<U> {
    private final List<List<U>> groups;
    private final double gap;
    /** Creates one filled cluster without grouping.
     * @param gap nonnegative separation between sphere surfaces */
    public SpherePacking3DLayout(double gap) { this(Collections.emptyList(),gap); }
    /** Creates grouped clusters; an empty grouping packs all nodes together.
     * @param groups disjoint node groups, or empty for a single cluster
     * @param gap nonnegative node and cluster surface separation */
    public SpherePacking3DLayout(List<? extends Collection<U>> groups,double gap) {
        if(!Double.isFinite(gap)||gap<0)throw new IllegalArgumentException("Sphere gap must be finite and non-negative");
        this.groups=new ArrayList<>();Set<U> seen=new HashSet<>();
        for(Collection<U> group:Objects.requireNonNull(groups)) {
            List<U> copy=new ArrayList<>();for(U node:group)
                if(node==null||!seen.add(node))throw new IllegalArgumentException("Sphere packing groups must contain unique, non-null nodes");else copy.add(node);
            if(!copy.isEmpty())this.groups.add(copy);
        }
        this.gap=gap;
    }
    @Override public LayoutDescriptor getDescriptor() { return new LayoutDescriptor("sphere-packing-3d","Sphere packing 3D",3); }
    @Override protected Map<U,Point3D> place(List<U> nodes,LayoutRequest<U> request) {
        Map<U,Point3D> result=new LinkedHashMap<>();int count=nodes.size();
        List<List<U>> clusters=groups.isEmpty()?Collections.singletonList(nodes):groups;
        Set<U> expected=new HashSet<>(nodes),grouped=new HashSet<>();clusters.forEach(grouped::addAll);
        if(!grouped.equals(expected))throw new IllegalArgumentException("Sphere packing groups must contain exactly the graph nodes");
        if(count==0)return result;
        List<Double> extents=new ArrayList<>();
        for(List<U> group:clusters) {
            checkpoint(request);
            double radius=0;for(U node:group)radius=Math.max(radius,request.getNodeSizes().getOrDefault(node,0.0));
            double pitch=2*radius+gap;if(pitch==0)pitch=1;
            if(!Double.isFinite(pitch))throw new IllegalArgumentException("Sphere packing scale overflow");
            List<Point3D> packed=pack(group.size(),pitch);
            double cx=0,cy=0,cz=0;for(Point3D p:packed){cx+=p.getX();cy+=p.getY();cz+=p.getZ();}
            cx/=packed.size();cy/=packed.size();cz/=packed.size();
            double extent=0;for(int i=0;i<group.size();i++) {
                U node=group.get(i);Point3D p=packed.get(i);double nodeRadius=request.getNodeSizes().getOrDefault(node,0.0);
                double dx=p.getX()-cx,dy=p.getY()-cy,dz=p.getZ()-cz;
                extent=Math.max(extent,Math.sqrt(dx*dx+dy*dy+dz*dz)+nodeRadius);
                result.put(node,new Point3D(dx,dy,dz));
            }
            extents.add(extent);
        }
        // Circle packing puts grouped discs around a ring. In 3D distribute
        // the filled clusters around a sphere, rather than along an axis.
        int m=clusters.size();if(m==1)return result;
        List<Point3D> directions=new ArrayList<>();
        double golden=Math.PI*(3-Math.sqrt(5));
        for(int i=0;i<m;i++) {
            if(m==2) directions.add(new Point3D(i==0?-1:1,0,0));
            else { double z=1-2*(i+.5)/m,r=Math.sqrt(Math.max(0,1-z*z)),angle=i*golden;
                directions.add(new Point3D(r*Math.cos(angle),r*Math.sin(angle),z)); }
        }
        double shellRadius=0;
        double largestExtent=Collections.max(extents);
        for(int i=0;i<m;i++) { checkpoint(request);for(int j=i-1;j>=0;j--) {
            Point3D a=directions.get(i),b=directions.get(j);
            // Directions are ordered by z. Once even the largest remaining
            // envelope fits using the z separation alone, earlier pairs fit too.
            if((b.getZ()-a.getZ())*shellRadius>extents.get(i)+largestExtent+gap)break;
            double distance=Math.hypot(Math.hypot(a.getX()-b.getX(),a.getY()-b.getY()),a.getZ()-b.getZ());
            shellRadius=Math.max(shellRadius,(extents.get(i)+extents.get(j)+gap)/distance);
        } }
        // Zero-radius singleton groups still need distinct centres.
        if(shellRadius==0)shellRadius=1;
        double centerX=0,centerY=0,centerZ=0;
        for(int i=0;i<m;i++) {
            Point3D direction=directions.get(i);double x=direction.getX()*shellRadius,y=direction.getY()*shellRadius,z=direction.getZ()*shellRadius;
            centerX+=x/m;centerY+=y/m;centerZ+=z/m;
            for(U node:clusters.get(i)) { Point3D p=result.get(node);result.put(node,new Point3D(p.getX()+x,p.getY()+y,p.getZ()+z)); }
        }
        final double cx=centerX,cy=centerY,cz=centerZ;
        result.replaceAll((node,p)->new Point3D(p.getX()-cx,p.getY()-cy,p.getZ()-cz));
        return result;
    }

    private static void checkpoint(LayoutRequest<?> request) {
        if(Thread.currentThread().isInterrupted()||request.getCancellation().getAsBoolean())throw new IllegalArgumentException("Sphere packing cancelled");
    }

    private List<Point3D> pack(int count,double pitch) {
        int span=(int)Math.ceil(Math.cbrt(count)/2)+2;double row=pitch*Math.sqrt(3)/2,layer=pitch*Math.sqrt(2.0/3.0);
        List<Point3D> sites=new ArrayList<>();
        for(int k=-span;k<=span;k++)for(int j=-span;j<=span;j++)for(int i=-span;i<=span;i++) {
            double x=(i+((j&1)==0?0:.5)+((k&1)==0?0:.5))*pitch;
            double y=j*row+((k&1)==0?0:row/3);
            sites.add(new Point3D(x,y,k*layer));
        }
        sites.sort(Comparator.comparingDouble((Point3D p)->p.getX()*p.getX()+p.getY()*p.getY()+p.getZ()*p.getZ())
            .thenComparingDouble(Point3D::getZ).thenComparingDouble(Point3D::getY).thenComparingDouble(Point3D::getX));
        Point3D first=sites.get(0);List<Point3D> result=new ArrayList<>();
        for(int i=0;i<count;i++) {Point3D p=sites.get(i);result.add(new Point3D(p.getX()-first.getX(),p.getY()-first.getY(),p.getZ()-first.getZ()));}
        return result;
    }
}
