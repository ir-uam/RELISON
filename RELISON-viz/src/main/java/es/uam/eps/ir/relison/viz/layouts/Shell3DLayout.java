/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.Layered3DPlacement;
import es.uam.eps.ir.relison.viz.internal.AbstractStaticLayout;
import java.util.*;

/**
 * Ordered shells on nested spheres or original 2D rings with z equal to the
 * zero-based shell number multiplied by zScale (one by default).
 * Membership and radii follow ShellLayout.
 * @param <U> node type
 */
public final class Shell3DLayout<U> implements Layout<U> {
    private final ShellLayout<U> flat;
    private final Map<U,Double> indices;
    private final Layered3DVariant variant;
    private final double zScale;
    /**
     * @param shells groups from inside to outside
     * @param spacing radial separation
     */
    public Shell3DLayout(List<? extends Collection<U>> shells,double spacing) { this(shells,spacing,Layered3DVariant.SPHERE); }
    /**
     * @param shells groups from inside to outside
     * @param spacing radial separation
     * @param variant sphere or cone
     */
    public Shell3DLayout(List<? extends Collection<U>> shells,double spacing,Layered3DVariant variant) {
        this(shells,spacing,variant,1);
    }
    /**
     * @param shells groups from inside to outside
     * @param spacing radial separation
     * @param variant sphere or cone
     * @param zScale positive cone-height multiplier; defaults to one in other constructors
     */
    public Shell3DLayout(List<? extends Collection<U>> shells,double spacing,Layered3DVariant variant,double zScale) {
        this.zScale=AbstractStaticLayout.positive(zScale,"zScale");
        flat=new ShellLayout<>(shells,spacing);this.variant=Objects.requireNonNull(variant);
        Map<U,Double> values=new LinkedHashMap<>();
        for(int i=0;i<shells.size();i++) for(U node:shells.get(i)) values.put(node,(double)i);
        indices=Collections.unmodifiableMap(values);
    }
    @Override public LayoutDescriptor getDescriptor() {
        return new LayoutDescriptor(variant==Layered3DVariant.SPHERE?"shell-sphere-3d":"shell-cone-3d",
            variant==Layered3DVariant.SPHERE?"Shell 3D (spheres)":"Shell 3D (cone)",3);
    }
    @Override public LayoutResult<U> compute(Graph<U> graph,LayoutRequest<U> request) {
        return Layered3DPlacement.compute(graph,request,flat,indices,variant,getDescriptor(),zScale);
    }
}
