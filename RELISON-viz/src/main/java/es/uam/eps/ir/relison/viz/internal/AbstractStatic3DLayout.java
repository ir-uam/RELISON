/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.internal;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.viz.*;
import java.util.*;

/**
 * Common ordering, validation and pin handling for spatial placements.
 * @param <U> node type
 */
public abstract class AbstractStatic3DLayout<U> implements Layout<U> {
    /**
     * @param nodes exact order
     * @param request inputs
     * @return coordinates covering every node
     */
    protected abstract Map<U,Point3D> place(List<U> nodes,LayoutRequest<U> request);
    @Override public final LayoutResult<U> compute(Graph<U> graph,LayoutRequest<U> request) {
        List<U> nodes=IndexedGraphSnapshot.capture(graph,request).getNodes();
        if(Thread.currentThread().isInterrupted()||request.getCancellation().getAsBoolean())
            throw new IllegalArgumentException("3D placement cancelled before computation");
        Map<U,Point3D> points=place(nodes,request);
        for(U node:request.getPinnedNodes()) {
            Point3D pin=request.getInitialPositions3D().get(node);
            if(pin==null) throw new IllegalArgumentException("Spatial pins require x/y/z coordinates");
            points.put(node,pin);
        }
        if(!points.keySet().equals(new HashSet<>(nodes))) throw new IllegalStateException("Incomplete 3D layout");
        return LayoutResult.spatial(points,new LayoutDiagnostics(getDescriptor().getId(),LayoutDiagnostics.Termination.COMPLETED));
    }
}
