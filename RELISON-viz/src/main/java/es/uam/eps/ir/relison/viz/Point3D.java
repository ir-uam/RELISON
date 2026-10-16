/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz;

import java.util.Objects;

/** Immutable finite coordinates in three-dimensional layout space. */
public final class Point3D {
    private final double x, y, z;
    /**
     * @param x x coordinate
     * @param y y coordinate
     * @param z z coordinate
     */
    public Point3D(double x, double y, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
            throw new IllegalArgumentException("3D coordinates must be finite");
        this.x = x; this.y = y; this.z = z;
    }
    /** @return x coordinate */
    public double getX() { return x; }
    /** @return y coordinate */
    public double getY() { return y; }
    /** @return z coordinate */
    public double getZ() { return z; }
    @Override public boolean equals(Object other) {
        if (!(other instanceof Point3D)) return false;
        Point3D p = (Point3D) other;
        return Double.compare(x, p.x) == 0 && Double.compare(y, p.y) == 0 && Double.compare(z, p.z) == 0;
    }
    @Override public int hashCode() { return Objects.hash(x, y, z); }
}
