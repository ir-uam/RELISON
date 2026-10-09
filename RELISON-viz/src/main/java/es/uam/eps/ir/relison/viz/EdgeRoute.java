/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz;

import es.uam.eps.ir.relison.utils.datatypes.Pair;
import java.util.*;

/**
 * Immutable polyline in original edge direction, including both node centres.
 * Parallel edges are distinguished by their zero-based occurrence per endpoint pair.
 * @param <U> node type
 */
public final class EdgeRoute<U>
{
    private final U source, target;
    private final int occurrence;
    private final List<Pair<Double>> points;

    /**
     * @param source original source
     * @param target original target
     * @param occurrence zero-based parallel-edge occurrence
     * @param points ordered polyline, including endpoints
     */
    public EdgeRoute(U source, U target, int occurrence, List<Pair<Double>> points)
    {
        this.source = Objects.requireNonNull(source);
        this.target = Objects.requireNonNull(target);
        if (occurrence < 0 || points.size() < 2) throw new IllegalArgumentException("Invalid edge route");
        this.occurrence = occurrence;
        List<Pair<Double>> copy = new ArrayList<>(points);
        Bounds2D.of(copy);
        this.points = Collections.unmodifiableList(copy);
    }
    /** @return original source */
    public U getSource() { return source; }
    /** @return original target */
    public U getTarget() { return target; }
    /** @return parallel-edge occurrence */
    public int getOccurrence() { return occurrence; }
    /** @return immutable polyline */
    public List<Pair<Double>> getPoints() { return points; }
}
