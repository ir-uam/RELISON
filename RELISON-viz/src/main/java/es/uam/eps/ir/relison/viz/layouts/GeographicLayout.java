/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.IndexedGraphSnapshot;
import java.util.*;
import java.util.function.ToDoubleFunction;

/**
 * Spherical Web Mercator or Equal Earth coordinates in metres.
 * Geographic inputs are decimal degrees; edges do not influence placement.
 * Mercator wraps without translating its origin; Equal Earth uses the supplied
 * central meridian as its origin. Latitude limits depend on the projection.
 * Pins must match projections.
 * @param <U> node type
 */
public final class GeographicLayout<U> implements Layout<U>
{
    /** WGS84 semi-major axis, in metres. */
    public static final double RADIUS = 6378137;
    /** Maximum latitude represented by the square Web Mercator tile world. */
    public static final double MAX_LATITUDE = 85.0511287798066;
    private final ToDoubleFunction<U> latitude, longitude;
    private final double centralLongitude;
    /** Supported spherical map projections. */
    public enum Projection { WEB_MERCATOR, EQUAL_EARTH }
    private final Projection projection;
    /**
     * @param latitude latitude in decimal degrees
     * @param longitude longitude in decimal degrees
     */
    public GeographicLayout(ToDoubleFunction<U> latitude, ToDoubleFunction<U> longitude)
    { this(latitude, longitude, 0); }
    /**
     * @param latitude latitude in decimal degrees
     * @param longitude longitude in decimal degrees
     * @param centralLongitude longitude around which to wrap, in decimal degrees
     */
    public GeographicLayout(ToDoubleFunction<U> latitude, ToDoubleFunction<U> longitude, double centralLongitude)
    { this(latitude, longitude, centralLongitude, Projection.WEB_MERCATOR); }
    /**
     * @param latitude latitude in decimal degrees
     * @param longitude longitude in decimal degrees
     * @param centralLongitude central meridian in decimal degrees
     * @param projection spherical projection to use
     */
    public GeographicLayout(ToDoubleFunction<U> latitude, ToDoubleFunction<U> longitude, double centralLongitude, Projection projection)
    {
        this.projection=Objects.requireNonNull(projection);
        this.latitude=Objects.requireNonNull(latitude); this.longitude=Objects.requireNonNull(longitude);
        if(!Double.isFinite(centralLongitude) || centralLongitude < -180 || centralLongitude > 180)
            throw new IllegalArgumentException("Central longitude must be between -180 and 180");
        this.centralLongitude=centralLongitude;
    }
    @Override public LayoutDescriptor getDescriptor() { return new LayoutDescriptor("geographic", "Geographic (" + (projection==Projection.EQUAL_EARTH?"Equal Earth":"Web Mercator") + ")"); }
    @Override public LayoutResult<U> compute(Graph<U> input, LayoutRequest<U> request)
    {
        IndexedGraphSnapshot<U> graph=IndexedGraphSnapshot.capture(input,request);
        Map<U,Pair<Double>> positions=new LinkedHashMap<>();
        boolean cancelled=false;
        for(U node:graph.getNodes())
        {
            cancelled |= Thread.currentThread().isInterrupted() || request.getCancellation().getAsBoolean();
            Pair<Double> projected=project(latitude.applyAsDouble(node),longitude.applyAsDouble(node),centralLongitude,projection);
            if(request.getPinnedNodes().contains(node) && !projected.equals(request.getInitialPositions().get(node)))
                throw new IllegalArgumentException("Geographic pins must match the projected location");
            positions.put(node,projected);
        }
        return new LayoutResult<>(positions,new LayoutDiagnostics("geographic",cancelled
            ?LayoutDiagnostics.Termination.CANCELLED:LayoutDiagnostics.Termination.COMPLETED));
    }
    /**
     * @param latitude latitude in decimal degrees
     * @param longitude longitude in decimal degrees
     * @param center central longitude in decimal degrees
     * @return projected metres with east-positive x and north-positive y
     */
    public static Pair<Double> project(double latitude,double longitude,double center)
    { return project(latitude,longitude,center,Projection.WEB_MERCATOR); }
    /**
     * Projects using the spherical Equal Earth equations of Savric, Patterson
     * and Jenny (2018), or Web Mercator. Equal Earth translates the central
     * meridian to x=0 and supports both poles.
     * @param latitude latitude in decimal degrees
     * @param longitude longitude in decimal degrees
     * @param center central meridian in decimal degrees
     * @param projection projection to use
     * @return east-positive x and north-positive y in metres
     */
    public static Pair<Double> project(double latitude,double longitude,double center,Projection projection)
    {
        Objects.requireNonNull(projection);
        double limit=projection==Projection.EQUAL_EARTH?90:MAX_LATITUDE;
        if(!Double.isFinite(latitude) || Math.abs(latitude)>limit)
            throw new IllegalArgumentException("Latitude must be finite and within +/-"+limit+" degrees");
        if(!Double.isFinite(longitude) || longitude < -180 || longitude > 180)
            throw new IllegalArgumentException("Longitude must be finite and between -180 and 180 degrees");
        if(!Double.isFinite(center) || center < -180 || center > 180)
            throw new IllegalArgumentException("Central longitude must be finite and between -180 and 180 degrees");
        double wrapped=center+((longitude-center+180)%360+360)%360-180;
        if(projection==Projection.EQUAL_EARTH)
        {
            double m=Math.sqrt(3)/2, theta=Math.asin(m*Math.sin(Math.toRadians(latitude)));
            double t2=theta*theta, t6=t2*t2*t2;
            double x=Math.toRadians(wrapped-center)*Math.cos(theta)/(m*(1.340264-3*.081106*t2+t6*(7*.000893+9*.003796*t2)));
            double y=theta*(1.340264-.081106*t2+t6*(.000893+.003796*t2));
            return new Pair<>(RADIUS*x,RADIUS*y);
        }
        double y=latitude==0?0:RADIUS*Math.log(Math.tan(Math.PI/4+Math.toRadians(latitude)/2));
        return new Pair<>(RADIUS*Math.toRadians(wrapped),y);
    }
}
