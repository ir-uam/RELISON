/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.*;
import java.util.*;
import java.util.function.ToDoubleFunction;

/**
 * Equal scores occupy nested spheres, or the original concentric 2D rings
 * lifted to z equal to the raw score times zScale (one by default).
 * Higher scores are innermost by default.
 * Scores are evaluated once per node; reversal affects radii, not heights.
 * @param <U> node type
 */
public final class Concentric3DLayout<U> implements Layout<U> {
    private final ToDoubleFunction<U> score;
    private final double spacing;
    private final boolean reverse;
    private final Layered3DVariant variant;
    private final double zScale;
    /**
     * @param score finite node scores; uses unit-spaced spheres
     */
    public Concentric3DLayout(ToDoubleFunction<U> score) { this(score,1); }
    /**
     * @param score finite node scores
     * @param spacing radial separation
     */
    public Concentric3DLayout(ToDoubleFunction<U> score,double spacing) { this(score,spacing,false,Layered3DVariant.SPHERE); }
    /**
     * @param score finite node scores
     * @param spacing radial separation
     * @param reverse whether lower scores are innermost; uses spheres
     */
    public Concentric3DLayout(ToDoubleFunction<U> score,double spacing,boolean reverse) { this(score,spacing,reverse,Layered3DVariant.SPHERE); }
    /**
     * @param score finite node scores
     * @param spacing radial separation
     * @param reverse whether lower scores are innermost
     * @param variant sphere or cone
     */
    public Concentric3DLayout(ToDoubleFunction<U> score,double spacing,boolean reverse,Layered3DVariant variant) {
        this(score,spacing,reverse,variant,1);
    }
    /**
     * @param score finite node scores
     * @param spacing radial separation
     * @param reverse whether lower scores are innermost
     * @param variant sphere or cone
     * @param zScale positive cone-height multiplier
     */
    public Concentric3DLayout(ToDoubleFunction<U> score,double spacing,boolean reverse,Layered3DVariant variant,double zScale) {
        this.zScale=AbstractStaticLayout.positive(zScale,"zScale");
        this.score=Objects.requireNonNull(score);this.spacing=AbstractStaticLayout.positive(spacing,"spacing");
        this.reverse=reverse;this.variant=Objects.requireNonNull(variant);
    }
    @Override public LayoutDescriptor getDescriptor() {
        return new LayoutDescriptor(variant==Layered3DVariant.SPHERE?"concentric-sphere-3d":"concentric-cone-3d",
            variant==Layered3DVariant.SPHERE?"Concentric by score 3D (spheres)":"Concentric by score 3D (cone)",3);
    }
    @Override public LayoutResult<U> compute(Graph<U> graph,LayoutRequest<U> request) {
        Map<U,Double> scores=new LinkedHashMap<>();
        for(U node:IndexedGraphSnapshot.capture(graph,request).getNodes()) {
            double value=score.applyAsDouble(node);
            if(!Double.isFinite(value)) throw new IllegalArgumentException("Scores must be finite");
            scores.put(node,value==0?0.0:value);
        }
        return Layered3DPlacement.compute(graph,request,new ConcentricLayout<>(scores::get,spacing,reverse),scores,variant,getDescriptor(),zScale);
    }
}
