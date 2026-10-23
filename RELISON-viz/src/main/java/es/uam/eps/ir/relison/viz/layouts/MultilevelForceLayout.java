/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.MultilevelForceSession;
import java.util.Objects;

/**
 * Edge-collapsing variant of Hu (2005), Efficient, High-Quality Force-Directed
 * Graph Drawing. Uses heavy-edge maximal matching, summed coarse edges,
 * sequential normalized force steps and coarse-to-fine refinement.
 * This implements the EC variant, not the paper's MIVS/hybrid variants.
 * Pins, iteration budgets and deterministic perturbations are extensions.
 * @param <U> node type
 */
public final class MultilevelForceLayout<U> implements IterativeLayout<U>
{
    private final MultilevelForceConfig config;
    /** Default Hu settings. */
    public MultilevelForceLayout() { this(new MultilevelForceConfig()); }
    /** @param config model settings */
    public MultilevelForceLayout(MultilevelForceConfig config) { this.config=Objects.requireNonNull(config); }
    @Override public LayoutDescriptor getDescriptor() { return new LayoutDescriptor("multilevel-force", "Multilevel force (Hu, EC)"); }
    @Override public LayoutSession<U> initialize(Graph<U> graph, LayoutRequest<U> request)
    { return new MultilevelForceSession<>(graph, request, config); }
}
