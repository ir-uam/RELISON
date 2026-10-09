/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.transforms;

import es.uam.eps.ir.relison.utils.datatypes.Pair;

import es.uam.eps.ir.relison.viz.*;
import java.util.*;

/**
 * Scales then translates unpinned coordinates. Pinned nodes stay in their
 * original layout space, so the output is not a global affine transform when
 * pins exist. Bounds are recomputed. Negative scales allow axis reflection.
 * @param <U> node type
 */
public final class CoordinateTransform<U> implements LayoutPostProcessor<U>
{
    private final double scaleX, scaleY, translateX, translateY;

    /**
     * @param scaleX horizontal scale
     * @param scaleY vertical scale
     * @param translateX horizontal translation
     * @param translateY vertical translation
     */
    public CoordinateTransform(double scaleX, double scaleY, double translateX, double translateY)
    {
        if (!Double.isFinite(scaleX) || !Double.isFinite(scaleY)
            || !Double.isFinite(translateX) || !Double.isFinite(translateY))
            throw new IllegalArgumentException("Transform parameters must be finite");
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.translateX = translateX;
        this.translateY = translateY;
    }

    @Override
    public LayoutResult<U> process(LayoutResult<U> result, LayoutRequest<U> request)
    {
        Objects.requireNonNull(result);
        Objects.requireNonNull(request);
        if (!result.getPositions().keySet().containsAll(request.getInitialPositions().keySet()))
            throw new IllegalArgumentException("Request references nodes outside the result");
        Map<U, Pair<Double>> positions = new LinkedHashMap<>();
        result.getPositions().forEach((node, point) -> positions.put(node,
            request.getPinnedNodes().contains(node) ? request.getInitialPositions().get(node)
                : new Pair<>(point.v1() * scaleX + translateX, point.v2() * scaleY + translateY)));
        List<EdgeRoute<U>> routes = new ArrayList<>();
        for (EdgeRoute<U> route : result.getEdgeRoutes())
        {
            List<Pair<Double>> points = new ArrayList<>();
            points.add(positions.get(route.getSource()));
            for (int i = 1; i < route.getPoints().size()-1; i++)
            {
                Pair<Double> p = route.getPoints().get(i);
                points.add(new Pair<>(p.v1()*scaleX+translateX, p.v2()*scaleY+translateY));
            }
            points.add(positions.get(route.getTarget()));
            routes.add(new EdgeRoute<>(route.getSource(), route.getTarget(), route.getOccurrence(), points));
        }
        return new LayoutResult<>(positions, result.getDiagnostics(), routes);
    }
}
