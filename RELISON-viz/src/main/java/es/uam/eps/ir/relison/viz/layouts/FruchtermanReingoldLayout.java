/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.ForceLayoutSession;
import java.util.Objects;
/**
 * Fruchterman-Reingold with simultaneous quadratic attraction, inverse-distance
 * repulsion and temperature-limited displacement, following the 1991 paper.
 * Default settings derive k from the drawing frame area, clamp coordinates to
 * its borders, and cool linearly over the iteration budget. The five-argument
 * config constructor selects the unbounded, geometrically cooled extension.
 * Seeded initialization, pins, weighted attraction and Barnes-Hut approximation
 * are supported; exact unweighted forces use theta zero and weighted false.
 * @param <U> node type
 */
public final class FruchtermanReingoldLayout<U> implements IterativeLayout<U> {
    private final FruchtermanReingoldConfig config;
    /** Default force settings. */
    public FruchtermanReingoldLayout() { this(new FruchtermanReingoldConfig()); }
    /** @param config immutable settings */
    public FruchtermanReingoldLayout(FruchtermanReingoldConfig config) { this.config=Objects.requireNonNull(config); }
    @Override
    public LayoutDescriptor getDescriptor() { return new LayoutDescriptor("fruchterman-reingold", "FruchtermanReingold (RELISON)"); }
    @Override
    public LayoutSession<U> initialize(Graph<U> graph, LayoutRequest<U> request) {
        return new ForceLayoutSession<>(graph, request, getDescriptor().getId(), config, null);
    }
}
