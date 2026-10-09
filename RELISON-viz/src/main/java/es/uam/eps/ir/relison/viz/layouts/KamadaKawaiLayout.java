/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.KamadaKawaiSession;
import java.util.Objects;
/**
 * Kamada and Kawai (1989), DOI 10.1016/0020-0190(89)90102-6: all-pairs springs,
 * l_ij = L d_ij and k_ij = K / d_ij squared, with L = drawingSize / diameter
 * per component. The maximum-gradient movable vertex is optimized by successive
 * 2D Newton steps before selecting another (the original nested-loop ordering).
 * One session iteration is one Newton step. Singular/non-descent steps use a
 * safeguarded descent direction and backtracking; this numerical safeguard and
 * exact pins extend the paper. Directed graphs are projected structurally.
 * Disconnected components have no mutual springs; packing is a separate processor.
 * @param <U> node type
 */
public final class KamadaKawaiLayout<U> implements IterativeLayout<U>
{
    private final KamadaKawaiConfig config;
    /** Uses default settings. */
    public KamadaKawaiLayout() { this(new KamadaKawaiConfig()); }
    /** @param config immutable settings */
    public KamadaKawaiLayout(KamadaKawaiConfig config) { this.config = Objects.requireNonNull(config); }
    @Override public LayoutDescriptor getDescriptor() { return new LayoutDescriptor("kamada-kawai", "Kamada-Kawai"); }
    @Override public LayoutSession<U> initialize(Graph<U> graph, LayoutRequest<U> request)
    { return new KamadaKawaiSession<>(graph, request, config); }
}
