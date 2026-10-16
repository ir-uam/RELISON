/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz;
import java.util.Collection;
import java.util.Objects;

/** Immutable axis-aligned spatial bounds; empty layouts have zero bounds. */
public final class Bounds3D {
    private final Point3D min, max;
    private Bounds3D(Point3D min, Point3D max) { this.min = min; this.max = max; }
    /**
     * @param points finite coordinates
     * @return their bounds
     */
    public static Bounds3D of(Collection<Point3D> points) {
        if (points.isEmpty()) return new Bounds3D(new Point3D(0,0,0), new Point3D(0,0,0));
        double ax=Double.POSITIVE_INFINITY, ay=ax, az=ax, bx=-ax, by=-ax, bz=-ax;
        for (Point3D p : points) {
            Objects.requireNonNull(p);
            ax=Math.min(ax,p.getX()); ay=Math.min(ay,p.getY()); az=Math.min(az,p.getZ());
            bx=Math.max(bx,p.getX()); by=Math.max(by,p.getY()); bz=Math.max(bz,p.getZ());
        }
        return new Bounds3D(new Point3D(ax,ay,az), new Point3D(bx,by,bz));
    }
    /** @return minimum coordinates */
    public Point3D getMin() { return min; }
    /** @return maximum coordinates */
    public Point3D getMax() { return max; }
}
