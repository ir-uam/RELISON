/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.FruchtermanReingold3DSession;
import java.util.Objects;

/** FR forces in R^3: attraction d^2/k, repulsion k^2/d, and temperature-limited
 * simultaneous displacement. Three-axis extension of Fruchterman/Reingold (1991).
 * Exact pairwise repulsion costs O(V^2+E) per iteration. Directed reciprocal
 * arcs and parallel edges accumulate; self-loops exert no force.
 * @param <U> node type */
public final class FruchtermanReingold3DLayout<U> implements IterativeLayout<U> {
    private final FruchtermanReingold3DConfig config;
    /** Default bounded settings. */
    public FruchtermanReingold3DLayout() { this(new FruchtermanReingold3DConfig()); }
    /** @param config immutable settings */
    public FruchtermanReingold3DLayout(FruchtermanReingold3DConfig config) { this.config=Objects.requireNonNull(config); }
    @Override public LayoutDescriptor getDescriptor() { return new LayoutDescriptor("fruchterman-reingold-3d","Fruchterman–Reingold 3D",3); }
    @Override public LayoutSession<U> initialize(Graph<U> graph,LayoutRequest<U> request) {
        return new FruchtermanReingold3DSession<>(graph,request,config);
    }
}
