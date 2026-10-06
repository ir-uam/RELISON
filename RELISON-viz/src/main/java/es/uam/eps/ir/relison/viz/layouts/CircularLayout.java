/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;

import es.uam.eps.ir.relison.utils.datatypes.Pair;

import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.*;
import java.util.*;

/**
 * Equal angular spacing around the origin; a sole node is placed at the origin.
 * @param <U> node type
 */
public final class CircularLayout<U> extends AbstractStaticLayout<U>
{
    private final double radius, startAngle;
    /** Creates a unit-radius circle starting on the positive x axis. */
    public CircularLayout() { this(1, 0); }
    /**
     * @param radius circle radius
     * @param startAngle initial angle in radians
     */
    public CircularLayout(double radius, double startAngle)
    {
        super("circular", "Circular");
        this.radius = positive(radius, "radius");
        if (!Double.isFinite(startAngle)) throw new IllegalArgumentException("startAngle must be finite");
        this.startAngle = Math.IEEEremainder(startAngle, 2 * Math.PI);
    }

    @Override
    protected Map<U, Pair<Double>> generate(IndexedGraphSnapshot<U> graph, LayoutRequest<U> request)
    {
        Map<U, Pair<Double>> positions = new LinkedHashMap<>();
        ring(graph.getNodes(), graph.getNodes().size() == 1 ? 0 : radius, startAngle, positions);
        return positions;
    }
}
