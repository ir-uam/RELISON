/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.IndexedGraphSnapshot;
import java.util.*;

/**
 * Munzner's H3 (1997), two-pass hemispherical layout, Appendix A, curvature -1.
 * Children are sorted by hemisphere radius and packed in concentric bands.
 * Output uses the projective (Klein) ball, multiplied by a display radius.
 * A supplied parent map chooses the spanning tree; otherwise deterministic BFS
 * chooses a spanning forest. A hidden super-root joins multiple forest roots.
 * Non-tree edges never exert forces. Pins are incompatible with this geometry.
 * @param <U> node type
 */
public final class H3Layout<U> implements Layout<U> {
    private final U root;
    private final EdgeOrientation direction;
    private final double displayRadius,leafRadius,areaScale;
    private final Map<U,U> parents;
    /** Uses BFS, undirected traversal, display radius 100, leaf radius .1 and area factor 7.2. */
    public H3Layout() { this(null,EdgeOrientation.UND,100,.1,7.2,Collections.emptyMap()); }
    /**
     * Creates a hyperbolic hemisphere layout with an optional supplied forest.
     * @param root first traversal root, or null for request order
     * @param direction traversal orientation for automatic tree selection
     * @param displayRadius radius of the displayed Klein ball
     * @param leafRadius positive hyperbolic leaf hemisphere radius
     * @param areaScale empirical hemisphere-area inflation, greater than 2
     * @param parents child-to-parent map defining the spanning forest, or empty for BFS
     */
    public H3Layout(U root,EdgeOrientation direction,double displayRadius,double leafRadius,double areaScale,Map<U,U> parents) {
        if(!Double.isFinite(displayRadius)||displayRadius<=0||!Double.isFinite(leafRadius)||leafRadius<=0
            ||!Double.isFinite(areaScale)||areaScale<=2) throw new IllegalArgumentException("Invalid H3 radii or area inflation");
        this.root=root;this.direction=Objects.requireNonNull(direction);this.displayRadius=displayRadius;
        this.leafRadius=leafRadius;this.areaScale=areaScale;this.parents=new LinkedHashMap<>(Objects.requireNonNull(parents));
    }
    @Override public LayoutDescriptor getDescriptor() { return new LayoutDescriptor("h3-3d","H3 hyperbolic hierarchy",3); }
    @Override public LayoutResult<U> compute(Graph<U> input,LayoutRequest<U> request) {
        IndexedGraphSnapshot<U> graph=IndexedGraphSnapshot.capture(input,request);
        if(!request.getPinnedNodes().isEmpty()) throw new IllegalArgumentException("H3 does not support arbitrary Euclidean pins");
        int n=graph.getNodes().size(); int first=root==null?0:graph.indexOf(root);
        if(first<0||(root!=null&&n==0)) throw new IllegalArgumentException("H3 root must be in the graph");
        List<Set<Integer>> out=new ArrayList<>(),in=new ArrayList<>();
        List<List<Integer>> children=new ArrayList<>();
        for(int i=0;i<n;i++) { out.add(new TreeSet<>());in.add(new TreeSet<>());children.add(new ArrayList<>()); }
        for(int i=0;i<n;i++) { check(request);for(int j:graph.getTargets(i)) if(i!=j) { out.get(i).add(j);in.get(j).add(i); } }
        int[] parent=new int[n];Arrays.fill(parent,-2);
        if(parents.isEmpty()) {
            for(int pass=0;pass<n;pass++) {
                int r=pass==0?first:(pass<=first?pass-1:pass); if(parent[r]!=-2) continue;
                parent[r]=-1;ArrayDeque<Integer> queue=new ArrayDeque<>();queue.add(r);
                while(!queue.isEmpty()) { check(request);int u=queue.remove();Set<Integer> adjacent=new TreeSet<>();
                    if(!graph.isDirected()||direction==EdgeOrientation.UND) { adjacent.addAll(out.get(u));adjacent.addAll(in.get(u)); }
                    else if(direction==EdgeOrientation.OUT) adjacent.addAll(out.get(u));
                    else if(direction==EdgeOrientation.IN) adjacent.addAll(in.get(u));
                    else { adjacent.addAll(out.get(u));adjacent.retainAll(in.get(u)); }
                    for(int v:adjacent) if(parent[v]==-2) { parent[v]=u;queue.add(v); }
                }
            }
        } else {
            Arrays.fill(parent,-1);
            for(Map.Entry<U,U> e:parents.entrySet()) {
                int child=graph.indexOf(e.getKey()),p=graph.indexOf(e.getValue());
                if(child<0||p<0||child==p||(!out.get(p).contains(child)&&!out.get(child).contains(p)))
                    throw new IllegalArgumentException("H3 parent map must contain existing graph edges");
                parent[child]=p;
            }
            if(root!=null&&parent[first]!=-1) throw new IllegalArgumentException("Chosen H3 root has a parent");
        }
        List<Integer> roots=new ArrayList<>();
        for(int i=0;i<n;i++) if(parent[i]<0) roots.add(i);else children.get(parent[i]).add(i);
        children.add(roots); int start=roots.size()==1?roots.get(0):n;
        List<Integer> traversal=new ArrayList<>();ArrayDeque<Integer> queue=new ArrayDeque<>();queue.add(start);
        while(!queue.isEmpty()) { check(request);int u=queue.remove();traversal.add(u);queue.addAll(children.get(u)); }
        if(traversal.size()!=(start==n?n+1:n)) throw new IllegalArgumentException("H3 parent map contains a cycle");
        double[] radius=new double[n+1];
        for(int k=traversal.size()-1;k>=0;k--) {
            check(request);int u=traversal.get(k);List<Integer> descendants=children.get(u);
            if(descendants.isEmpty()) radius[u]=leafRadius;
            else {
                double area=0;for(int v:descendants) area+=2*Math.pow(Math.sinh(radius[v]/2),2);
                radius[u]=asinh(Math.sqrt(areaScale*area));
                if(!Double.isFinite(radius[u])) throw new IllegalArgumentException("H3 hemisphere radius overflow");
                descendants.sort(Comparator.<Integer>comparingDouble(v->radius[v]).reversed().thenComparingInt(v->v));
            }
        }
        double[][] frames=new double[n+1][];frames[start]=identity();Map<U,Point3D> points=new LinkedHashMap<>();
        for(int u:traversal) {
            check(request);double[] frame=frames[u];
            if(u<n) {
                double x=frame[3]/frame[15],y=frame[7]/frame[15],z=frame[11]/frame[15];
                if(Math.hypot(Math.hypot(x,y),z)>=1-1e-12)
                    throw new IllegalArgumentException("H3 hierarchy exceeds double-precision Klein-ball resolution; choose a shallower spanning hierarchy");
                points.put(graph.getNodes().get(u),new Point3D(displayRadius*x,displayRadius*y,displayRadius*z));
            }
            List<Integer> list=children.get(u);if(list.isEmpty()) continue;
            double phi=0,thetaEnd=0,bandHalf=0;
            for(int k=0;k<list.size();k++) {
                int v=list.get(k);double half=Math.atan(Math.tanh(radius[v])/Math.sinh(radius[u])),theta=0;
                if(k==0) bandHalf=half;
                else {
                    if(k==1) { phi=bandHalf+half;bandHalf=half;thetaEnd=0; }
                    double dt=Math.atan(Math.tanh(radius[v])/(Math.sinh(radius[u])*Math.sin(phi)));
                    if(thetaEnd+2*dt>2*Math.PI) { phi+=bandHalf+half;bandHalf=half;thetaEnd=0;
                        dt=Math.atan(Math.tanh(radius[v])/(Math.sinh(radius[u])*Math.sin(phi))); }
                    if(phi+half>Math.PI/2) throw new IllegalArgumentException("H3 bands exceed a hemisphere; increase the area inflation factor");
                    theta=thetaEnd+dt;thetaEnd=theta+dt;
                }
                frames[v]=multiply(frame,multiply(rotation(phi,theta),boost(radius[u])));
            }
            frames[u]=null;
        }
        return LayoutResult.spatial(points,new LayoutDiagnostics(getDescriptor().getId(),LayoutDiagnostics.Termination.COMPLETED));
    }
    private static double asinh(double x) { return Math.log(x+Math.hypot(x,1)); }
    private static double[] identity() { return new double[]{1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1}; }
    private static double[] boost(double distance) { double[] m=identity();double c=Math.cosh(distance),s=Math.sinh(distance);m[10]=m[15]=c;m[11]=m[14]=s;return m; }
    private static double[] rotation(double phi,double theta) {
        double c=Math.cos(phi),s=Math.sin(phi),a=Math.cos(theta),b=Math.sin(theta);
        return new double[]{a*c,-b,a*s,0,b*c,a,b*s,0,-s,0,c,0,0,0,0,1};
    }
    private static double[] multiply(double[] a,double[] b) { double[] c=new double[16];for(int i=0;i<4;i++) for(int j=0;j<4;j++) for(int k=0;k<4;k++) c[4*i+j]+=a[4*i+k]*b[4*k+j];return c; }
    private static void check(LayoutRequest<?> request) { if(Thread.currentThread().isInterrupted()||request.getCancellation().getAsBoolean()) throw new IllegalArgumentException("H3 layout cancelled"); }
}
