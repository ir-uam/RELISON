/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.AbstractStatic3DLayout;
import java.util.*;
/**
 * Equal-area latitude bands and golden-angle placement on a sphere, independent of topology.
 * @param <U> node type
 */
public final class SphericalLayout<U> extends AbstractStatic3DLayout<U> {
    private final double radius;
    /** Default radius 100. */
    public SphericalLayout() { this(100); }
    /** @param radius positive radius */
    public SphericalLayout(double radius) {
        if(!Double.isFinite(radius)||radius<=0) throw new IllegalArgumentException("Invalid sphere radius");this.radius=radius;
    }
    @Override public LayoutDescriptor getDescriptor() { return new LayoutDescriptor("spherical-3d","Spherical 3D",3); }
    @Override protected Map<U,Point3D> place(List<U> nodes,LayoutRequest<U> request) {
        Map<U,Point3D> result=new LinkedHashMap<>();
        for(int i=0;i<nodes.size();i++) {
            if(nodes.size()==1) { result.put(nodes.get(i),new Point3D(0,0,0));break; }
            double y=1-2*(i+.5)/nodes.size(),r=Math.sqrt(Math.max(0,1-y*y)),angle=i*Math.PI*(3-Math.sqrt(5));
            result.put(nodes.get(i),new Point3D(radius*r*Math.cos(angle),radius*y,radius*r*Math.sin(angle)));
        }
        return result;
    }
}
