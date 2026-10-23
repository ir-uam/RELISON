/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.SphericalMdsSession;
import java.util.Objects;

/**
 * Spherical MDS, Miller, Huroyan and Kobourov (2022), Algorithm 1 and Eq. 4.
 * Uses angular-coordinate SGD, uniform pair weights, diameter dilation and
 * random reshuffling. Output is Cartesian coordinates on a sphere, not a
 * normalized Euclidean stress layout. Requires a connected undirected projection.
 * @param <U> node type
 */
public final class SphericalMdsLayout<U> implements IterativeLayout<U> {
    private final SphericalMdsConfig config;
    /** Uses the paper's default schedule and diameter heuristic. */
    public SphericalMdsLayout() { this(new SphericalMdsConfig()); }
    /** Creates a configured spherical MDS layout.
     * @param config settings */
    public SphericalMdsLayout(SphericalMdsConfig config) { this.config=Objects.requireNonNull(config); }
    @Override public LayoutDescriptor getDescriptor() { return new LayoutDescriptor("spherical-mds-3d","Spherical MDS (Miller et al.)",3); }
    @Override public LayoutSession<U> initialize(Graph<U> graph,LayoutRequest<U> request) { return new SphericalMdsSession<>(graph,request,config); }
}
