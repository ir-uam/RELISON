/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz;

import es.uam.eps.ir.relison.utils.datatypes.Pair;

import java.util.*;

/**
 * Immutable node coordinates, optional edge polylines and their combined bounds.
 * @param <U> node type
 */
public final class LayoutResult<U>
{
    private final Map<U, Pair<Double>> positions;
    private final Bounds2D bounds;
    private final LayoutDiagnostics diagnostics;
    private final List<EdgeRoute<U>> edgeRoutes;

    /**
     * @param positions node coordinates
     * @param diagnostics termination information
     */
    public LayoutResult(Map<U, Pair<Double>> positions, LayoutDiagnostics diagnostics)
    {
        this(positions, diagnostics, Collections.emptyList());
    }

    /**
     * @param positions node coordinates
     * @param diagnostics termination information
     * @param edgeRoutes edge polylines in original edge direction
     */
    public LayoutResult(Map<U, Pair<Double>> positions, LayoutDiagnostics diagnostics, List<EdgeRoute<U>> edgeRoutes)
    {
        Map<U, Pair<Double>> copy = new LinkedHashMap<>(positions);
        copy.forEach((node, point) -> { Objects.requireNonNull(node); Objects.requireNonNull(point); });
        this.positions = Collections.unmodifiableMap(copy);
        List<EdgeRoute<U>> routes = new ArrayList<>(edgeRoutes);
        List<Pair<Double>> allPoints = new ArrayList<>(copy.values());
        for (EdgeRoute<U> route : routes)
        {
            Objects.requireNonNull(route);
            if (!copy.containsKey(route.getSource()) || !copy.containsKey(route.getTarget()))
                throw new IllegalArgumentException("Route endpoints must exist in the layout");
            List<Pair<Double>> points = route.getPoints();
            if (!points.get(0).equals(copy.get(route.getSource())) || !points.get(points.size()-1).equals(copy.get(route.getTarget())))
                throw new IllegalArgumentException("Route endpoints must match node coordinates");
            allPoints.addAll(points);
        }
        this.edgeRoutes = Collections.unmodifiableList(routes);
        this.bounds = Bounds2D.of(allPoints);
        this.diagnostics = Objects.requireNonNull(diagnostics);
    }
    /** @return immutable coordinates in layout order */
    public Map<U, Pair<Double>> getPositions() { return positions; }
    /** @return coordinate bounds */
    public Bounds2D getBounds() { return bounds; }
    /** @return computation diagnostics */
    public LayoutDiagnostics getDiagnostics() { return diagnostics; }
    /** @return immutable routes; empty for layouts without edge routing */
    public List<EdgeRoute<U>> getEdgeRoutes() { return edgeRoutes; }
}
