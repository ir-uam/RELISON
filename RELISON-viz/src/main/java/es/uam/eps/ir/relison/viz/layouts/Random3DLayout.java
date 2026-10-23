/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.AbstractStatic3DLayout;
import java.util.*;
/**
 * Seeded uniform placement in a centred cuboid.
 * @param <U> node type
 */
public final class Random3DLayout<U> extends AbstractStatic3DLayout<U> {
    private final double width,height,depth;
    /** 600-unit cube. */
    public Random3DLayout() { this(600,600,600); }
    /**
     * @param width positive width
     * @param height positive height
     * @param depth positive depth
     */
    public Random3DLayout(double width,double height,double depth) {
        for(double v:new double[]{width,height,depth}) if(!Double.isFinite(v)||v<=0) throw new IllegalArgumentException("Invalid 3D random extent");
        this.width=width;this.height=height;this.depth=depth;
    }
    @Override public LayoutDescriptor getDescriptor() { return new LayoutDescriptor("random-3d","Random 3D",3); }
    @Override protected Map<U,Point3D> place(List<U> nodes,LayoutRequest<U> request) {
        Map<U,Point3D> result=new LinkedHashMap<>();Random random=new Random(request.getSeed());
        for(U node:nodes) result.put(node,new Point3D((random.nextDouble()-.5)*width,(random.nextDouble()-.5)*height,(random.nextDouble()-.5)*depth));
        return result;
    }
}
