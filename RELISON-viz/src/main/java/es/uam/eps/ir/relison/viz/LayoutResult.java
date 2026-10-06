/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz;

import es.uam.eps.ir.relison.utils.datatypes.Pair;

import java.util.*;

/**
 * Immutable node coordinates and their derived bounds.
 * @param <U> node type
 */
public final class LayoutResult<U>
{
    private final Map<U, Pair<Double>> positions;
    private final Bounds2D bounds;
    private final LayoutDiagnostics diagnostics;

    /**
     * @param positions node coordinates
     * @param diagnostics termination information
     */
    public LayoutResult(Map<U, Pair<Double>> positions, LayoutDiagnostics diagnostics)
    {
        Map<U, Pair<Double>> copy = new LinkedHashMap<>(positions);
        copy.forEach((node, point) -> { Objects.requireNonNull(node); Objects.requireNonNull(point); });
        this.positions = Collections.unmodifiableMap(copy);
        this.bounds = Bounds2D.of(copy.values());
        this.diagnostics = Objects.requireNonNull(diagnostics);
    }
    /** @return immutable coordinates in layout order */
    public Map<U, Pair<Double>> getPositions() { return positions; }
    /** @return coordinate bounds */
    public Bounds2D getBounds() { return bounds; }
    /** @return computation diagnostics */
    public LayoutDiagnostics getDiagnostics() { return diagnostics; }
}
