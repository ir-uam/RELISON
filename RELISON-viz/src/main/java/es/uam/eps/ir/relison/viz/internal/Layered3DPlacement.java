/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.internal;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.layouts.Layered3DVariant;
import java.util.*;

/** Shared sphere placement and exact 2D-to-cone lifting. */
public final class Layered3DPlacement {
    private Layered3DPlacement() { }

    /**
     * @param graph input graph
     * @param request common ordering and spatial pins
     * @param flat original 2D algorithm
     * @param heights group index, score or hops for every node
     * @param variant sphere or cone
     * @param descriptor spatial algorithm identity
     * @param <U> node type
     * @return complete spatial placement
     */
    public static <U> LayoutResult<U> compute(Graph<U> graph, LayoutRequest<U> request, Layout<U> flat,
            Map<U,Double> heights, Layered3DVariant variant, LayoutDescriptor descriptor) {
        return compute(graph,request,flat,heights,variant,descriptor,1);
    }

    /**
     * @param graph input graph
     * @param request ordering and spatial pins
     * @param flat original 2D algorithm
     * @param heights group index, score or hops for each node
     * @param variant sphere or cone
     * @param descriptor spatial identity
     * @param zScale positive finite multiplier for cone heights; spheres are unchanged
     * @param <U> node type
     * @return complete spatial placement
     */
    public static <U> LayoutResult<U> compute(Graph<U> graph, LayoutRequest<U> request, Layout<U> flat,
            Map<U,Double> heights, Layered3DVariant variant, LayoutDescriptor descriptor, double zScale) {
        AbstractStaticLayout.positive(zScale,"zScale");
        Objects.requireNonNull(variant);
        Objects.requireNonNull(descriptor);
        List<U> order = IndexedGraphSnapshot.capture(graph,request).getNodes();
        if(!heights.keySet().equals(new HashSet<>(order))) throw new IllegalArgumentException("Layer values must cover every node");
        heights.values().forEach(value -> {
            if(value==null || !Double.isFinite(value)) throw new IllegalArgumentException("Layer values must be finite");
        });
        // Pins are applied after the geometry is built, so they cannot alter
        // another node's sphere radius or its angular position.
        LayoutRequest<U> unpinned = LayoutRequest.<U>builder().nodeOrder(order).seed(request.getSeed()).build();
        Map<U,Pair<Double>> xy = flat.compute(graph,unpinned).getPositions();
        Map<U,Point3D> points = new LinkedHashMap<>();
        if(variant==Layered3DVariant.CONE) {
            for(U node:order) {
                Pair<Double> p=xy.get(node);
                points.put(node,new Point3D(p.v1(),p.v2(),heights.get(node)*zScale));
            }
        } else {
            Map<Double,List<U>> groups=new LinkedHashMap<>();
            for(U node:order) {
                double level=heights.get(node);
                groups.computeIfAbsent(level==0?0.0:level,key->new ArrayList<>()).add(node);
            }
            for(List<U> group:groups.values()) {
                Pair<Double> first=xy.get(group.get(0));
                double radius=Math.hypot(first.v1(),first.v2());
                for(int i=0;i<group.size();i++) {
                    double latitude=1-2*(i+.5)/group.size(), angle=i*Math.PI*(3-Math.sqrt(5));
                    double radial=Math.sqrt(Math.max(0,1-latitude*latitude));
                    points.put(group.get(i),new Point3D(radius*radial*Math.cos(angle),radius*latitude,radius*radial*Math.sin(angle)));
                }
            }
        }
        Map<U,Point3D> ordered=new LinkedHashMap<>();
        for(U node:order) {
            Point3D pin=request.getPinnedNodes().contains(node)?request.getInitialPositions3D().get(node):null;
            if(request.getPinnedNodes().contains(node)&&pin==null) throw new IllegalArgumentException("Spatial pins require x/y/z coordinates");
            ordered.put(node,pin==null?points.get(node):pin);
        }
        boolean cancelled=Thread.currentThread().isInterrupted()||request.getCancellation().getAsBoolean();
        return LayoutResult.spatial(ordered,new LayoutDiagnostics(descriptor.getId(),cancelled
            ? LayoutDiagnostics.Termination.CANCELLED:LayoutDiagnostics.Termination.COMPLETED));
    }
}
