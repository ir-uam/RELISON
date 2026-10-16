/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.AbstractStatic3DLayout;
import java.util.*;
/**
 * Centred rectangular lattice, x then y then z; a partial final layer retains
 * alignment. With both axis capacities automatic, retains the cubic default.
 * @param <U> node type
 */
public final class Grid3DLayout<U> extends AbstractStatic3DLayout<U> {
    private final double spacing;
    private final int columns,depth;
    /** Default spacing 50. */
    public Grid3DLayout() { this(50); }
    /** @param spacing positive spacing */
    public Grid3DLayout(double spacing) {
        this(0,0,spacing);
    }
    /** Creates a grid with configurable axis capacities.
     * @param columns x columns, or zero for automatic
     * @param depth z layers, or zero for automatic
     * @param spacing positive axis spacing
     */
    public Grid3DLayout(int columns,int depth,double spacing) {
        if(columns<0||depth<0)throw new IllegalArgumentException("Grid columns and depth must be nonnegative");
        this.columns=columns;this.depth=depth;
        if(!Double.isFinite(spacing)||spacing<=0) throw new IllegalArgumentException("Invalid grid spacing");this.spacing=spacing;
    }
    @Override public LayoutDescriptor getDescriptor() { return new LayoutDescriptor("grid-3d","Grid 3D",3); }
    @Override protected Map<U,Point3D> place(List<U> nodes,LayoutRequest<U> request) {
        Map<U,Point3D> result=new LinkedHashMap<>();
        int count=nodes.size();if(count==0)return result;
        int side=Math.max(1,(int)Math.ceil(Math.cbrt(count)));
        int cols=columns==0?(depth==0?side:Math.max(1,(int)Math.ceil(Math.sqrt((double)count/depth)))):columns;
        int rows=depth==0?(columns==0?side:Math.max(1,(int)Math.ceil(Math.sqrt((double)count/cols))))
            :Math.max(1,(int)Math.ceil((double)count/cols/depth));
        int layers=(int)Math.ceil((double)count/cols/rows);
        for(int i=0;i<nodes.size();i++) result.put(nodes.get(i),new Point3D(
            (i%cols-(cols-1)/2.0)*spacing,((i/cols)%rows-(rows-1)/2.0)*spacing,(i/cols/rows-(layers-1)/2.0)*spacing));
        return result;
    }
}
