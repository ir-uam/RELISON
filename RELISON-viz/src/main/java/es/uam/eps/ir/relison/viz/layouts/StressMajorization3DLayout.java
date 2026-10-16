/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.StressMajorizationSession;
import java.util.Objects;

/** Full-stress majorization in R^3, following Gansner, Koren and North (2004).
 * Each axis solves the same constrained graph Laplacian. Requires quadratic
 * storage and dense factorization per connected component.
 * @param <U> node type */
public final class StressMajorization3DLayout<U> implements IterativeLayout<U> {
    private final StressMajorizationConfig config;
    /** Default stress settings. */
    public StressMajorization3DLayout() { this(new StressMajorizationConfig()); }
    /** @param config stress settings */
    public StressMajorization3DLayout(StressMajorizationConfig config) { this.config=Objects.requireNonNull(config); }
    @Override public LayoutDescriptor getDescriptor() { return new LayoutDescriptor("stress-majorization-3d","Stress majorization 3D",3); }
    @Override public LayoutSession<U> initialize(Graph<U> graph,LayoutRequest<U> request) {
        return new StressMajorizationSession<>(graph,request,config,true);
    }
}
