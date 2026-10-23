/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.IndexedGraphSnapshot;
import java.util.*;

/**
 * Dujmovic and Wood (2005), Theorem 3.1: a topologically ordered vertex i
 * is placed at (i^3 mod p, i^2 mod p, i), for the first prime p greater than n.
 * This is the upward grid construction, not a Sugiyama layering heuristic.
 * The drawing has no intersections of nonincident straight edges in exact
 * arithmetic. Parallel edges necessarily coincide. Cycles and pins are rejected.
 * @param <U> node type
 */
public final class UpwardGrid3DLayout<U> implements Layout<U> {
    private final double spacing;
    /** Uses unit grid spacing. */
    public UpwardGrid3DLayout() { this(1); }
    /** Creates a uniformly scaled upward grid.
     * @param spacing positive uniform grid scale */
    public UpwardGrid3DLayout(double spacing) {
        if(!Double.isFinite(spacing)||spacing<=0) throw new IllegalArgumentException("Grid spacing must be positive and finite");
        this.spacing=spacing;
    }
    @Override public LayoutDescriptor getDescriptor() { return new LayoutDescriptor("upward-grid-3d","Upward grid 3D (Dujmovic–Wood)",3); }
    @Override public LayoutResult<U> compute(Graph<U> input,LayoutRequest<U> request) {
        IndexedGraphSnapshot<U> graph=IndexedGraphSnapshot.capture(input,request);
        if(!graph.isDirected()) throw new IllegalArgumentException("Upward grid requires a directed acyclic graph");
        if(!request.getPinnedNodes().isEmpty()) throw new IllegalArgumentException("Upward grid cannot preserve arbitrary pins");
        int n=graph.getNodes().size(); int[][] arcs=new int[n][]; int[] indegree=new int[n];
        for(int i=0;i<n;i++) { check(request); arcs[i]=graph.getTargets(i); for(int j:arcs[i]) indegree[j]++; }
        PriorityQueue<Integer> ready=new PriorityQueue<>(); for(int i=0;i<n;i++) if(indegree[i]==0) ready.add(i);
        List<Integer> order=new ArrayList<>();
        while(!ready.isEmpty()) { check(request); int i=ready.remove(); order.add(i); for(int j:arcs[i]) if(--indegree[j]==0) ready.add(j); }
        if(order.size()!=n) throw new IllegalArgumentException("Upward grid requires a DAG; directed cycles and self-loops are not allowed");
        long prime=Math.max(2,(long)n+1); while(!prime(prime)) { check(request); prime++; }
        Map<U,Point3D> points=new LinkedHashMap<>();
        for(int k=0;k<n;k++) { check(request); long i=k+1L, square=(i*i)%prime;
            points.put(graph.getNodes().get(order.get(k)),new Point3D((square*i%prime)*spacing,square*spacing,i*spacing)); }
        return LayoutResult.spatial(points,new LayoutDiagnostics(getDescriptor().getId(),LayoutDiagnostics.Termination.COMPLETED));
    }
    private static boolean prime(long p) { for(long d=2;d*d<=p;d++) if(p%d==0) return false; return true; }
    private static void check(LayoutRequest<?> request) {
        if(Thread.currentThread().isInterrupted()||request.getCancellation().getAsBoolean()) throw new IllegalArgumentException("Upward grid cancelled");
    }
}
