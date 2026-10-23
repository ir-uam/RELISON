/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.ForceLayoutSession;
import java.util.Objects;
/**
 * ForceAtlas2 with degree mass, linear or LinLog attraction, gravity and
 * adaptive global speed efficiency with local swinging damping. Standard mode
 * has no fixed movement cap. Optional collision mode uses request node radii.
 * Sessions support seeded initialization, exact pins and optional Barnes-Hut
 * repulsion. Theta zero selects the exact pairwise force model.
 *
 * <p>The equations follow Jacomy et al. (2014) and the Gephi reference algorithm;
 * seeded double-precision coordinates and accumulation order need not match
 * Gephi's output. Iteration limits and convergence tolerance are session controls.</p>
 * @param <U> node type
 */
public final class ForceAtlas2Layout<U> implements IterativeLayout<U> {
    private final ForceAtlas2Config config;
    /** Default force settings. */
    public ForceAtlas2Layout() { this(new ForceAtlas2Config()); }
    /** @param config immutable settings */
    public ForceAtlas2Layout(ForceAtlas2Config config) { this.config=Objects.requireNonNull(config); }
    @Override
    public LayoutDescriptor getDescriptor() { return new LayoutDescriptor("relison-forceatlas2", "ForceAtlas2 (RELISON)"); }
    @Override
    public LayoutSession<U> initialize(Graph<U> graph, LayoutRequest<U> request) {
        return new ForceLayoutSession<>(graph, request, getDescriptor().getId(), null, config);
    }
}
