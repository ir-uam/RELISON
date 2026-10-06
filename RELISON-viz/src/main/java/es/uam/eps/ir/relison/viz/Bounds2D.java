/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz;

import es.uam.eps.ir.relison.utils.datatypes.Pair;

import java.util.Collection;

/** Coordinate bounds. An empty layout has zero bounds at the origin. */
public final class Bounds2D
{
    private final double minX, minY, maxX, maxY;

    private Bounds2D(double minX, double minY, double maxX, double maxY)
    {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    /**
     * @param points coordinates
     * @return their bounding box
     */
    public static Bounds2D of(Collection<Pair<Double>> points)
    {
        if (points.isEmpty()) return new Bounds2D(0, 0, 0, 0);
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (Pair<Double> point : points)
        {
            if (point == null || point.v1() == null || point.v2() == null
                || !Double.isFinite(point.v1()) || !Double.isFinite(point.v2()))
                throw new IllegalArgumentException("Coordinates must contain two finite doubles");
            minX = Math.min(minX, point.v1());
            minY = Math.min(minY, point.v2());
            maxX = Math.max(maxX, point.v1());
            maxY = Math.max(maxY, point.v2());
        }
        return new Bounds2D(minX, minY, maxX, maxY);
    }

    /** @return minimum x */
    public double getMinX() { return minX; }
    /** @return minimum y */
    public double getMinY() { return minY; }
    /** @return maximum x */
    public double getMaxX() { return maxX; }
    /** @return maximum y */
    public double getMaxY() { return maxY; }
}
