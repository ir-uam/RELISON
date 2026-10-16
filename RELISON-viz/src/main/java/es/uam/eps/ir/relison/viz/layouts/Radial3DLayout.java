/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.*;
import java.util.*;

/**
 * Hop-distance spheres, or original radial 2D rings lifted to z equal to hops
 * multiplied by zScale (one by default).
 * The root is at z=0 in cone mode. Unreachable nodes retain the extra outer
 * ring/sphere from RadialLayout and use z=-zScale in cone mode (not a hop distance).
 * Weights and parallel multiplicity do not affect traversal.
 * @param <U> node type
 */
public final class Radial3DLayout<U> implements Layout<U> {
    private final U root;
    private final EdgeOrientation direction;
    private final double spacing;
    private final Layered3DVariant variant;
    private final double zScale;
    /** Default root, undirected traversal and unit-spaced spheres. */
    public Radial3DLayout() { this(null,EdgeOrientation.UND,1,Layered3DVariant.SPHERE); }
    /**
     * @param root root or null for first node in request order
     * @param direction BFS traversal direction
     * @param spacing radial separation per hop; uses spheres
     */
    public Radial3DLayout(U root,EdgeOrientation direction,double spacing) { this(root,direction,spacing,Layered3DVariant.SPHERE); }
    /**
     * @param root root or null for first node in request order
     * @param direction BFS traversal direction
     * @param spacing radial separation per hop
     * @param variant sphere or cone
     */
    public Radial3DLayout(U root,EdgeOrientation direction,double spacing,Layered3DVariant variant) {
        this(root,direction,spacing,variant,1);
    }
    /**
     * @param root root or null for first node in request order
     * @param direction BFS traversal direction
     * @param spacing radial separation per hop
     * @param variant sphere or cone
     * @param zScale positive cone-height multiplier, including the unreachable sentinel
     */
    public Radial3DLayout(U root,EdgeOrientation direction,double spacing,Layered3DVariant variant,double zScale) {
        this.zScale=AbstractStaticLayout.positive(zScale,"zScale");
        this.root=root;this.direction=Objects.requireNonNull(direction);
        this.spacing=AbstractStaticLayout.positive(spacing,"spacing");this.variant=Objects.requireNonNull(variant);
    }
    @Override public LayoutDescriptor getDescriptor() {
        return new LayoutDescriptor(variant==Layered3DVariant.SPHERE?"radial-sphere-3d":"radial-cone-3d",
            variant==Layered3DVariant.SPHERE?"Radial / ego 3D (spheres)":"Radial / ego 3D (cone)",3);
    }
    @Override public LayoutResult<U> compute(Graph<U> graph,LayoutRequest<U> request) {
        IndexedGraphSnapshot<U> snapshot=IndexedGraphSnapshot.capture(graph,request);
        int count=snapshot.getNodes().size(),start=root==null?0:snapshot.indexOf(root);
        if(start<0||(root!=null&&count==0)) throw new IllegalArgumentException("Radial root must exist in the graph");
        int[] hops=new int[count];Arrays.fill(hops,-1);
        if(count>0) {
            int[][] adjacent=Topology.neighbours(snapshot,direction);
            Deque<Integer> queue=new ArrayDeque<>();hops[start]=0;queue.add(start);
            while(!queue.isEmpty()) {
                int node=queue.remove();
                for(int neighbour:adjacent[node]) if(hops[neighbour]<0) { hops[neighbour]=hops[node]+1;queue.add(neighbour); }
            }
        }
        Map<U,Double> heights=new LinkedHashMap<>();
        for(int i=0;i<count;i++) heights.put(snapshot.getNodes().get(i),(double)hops[i]);
        return Layered3DPlacement.compute(graph,request,new RadialLayout<>(root,direction,spacing),heights,variant,getDescriptor(),zScale);
    }
}
