/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.StressMajorizationSession;
import java.util.Objects;
/**
 * Full stress majorization, solving Lw X = Lz Z as in Gansner, Koren and North,
 * Graph Drawing 2004, DOI 10.1007/978-3-540-31843-9_25 (equations 8-10).
 * Uses reusable reduced Cholesky factors per weak component. Pins are fixed
 * Dirichlet constraints; an unpinned component fixes its first node to remove
 * translation freedom. Unreachable pairs are omitted, not assigned fake distances.
 * Directed edges use an undirected projection; parallel lengths use their minimum.
 * This is the full O(V squared) model, not sparse or subspace stress.
 * @param <U> node type
 */
public final class StressMajorizationLayout<U> implements IterativeLayout<U>
{
    private final StressMajorizationConfig config;
    /** Uses default settings. */
    public StressMajorizationLayout() { this(new StressMajorizationConfig()); }
    /** @param config immutable settings */
    public StressMajorizationLayout(StressMajorizationConfig config) { this.config = Objects.requireNonNull(config); }
    @Override public LayoutDescriptor getDescriptor() { return new LayoutDescriptor("stress-majorization", "Stress majorization"); }
    @Override public LayoutSession<U> initialize(Graph<U> graph, LayoutRequest<U> request)
    { return new StressMajorizationSession<>(graph, request, config); }
}
