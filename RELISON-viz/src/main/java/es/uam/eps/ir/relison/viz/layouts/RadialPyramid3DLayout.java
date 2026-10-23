/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.*;
import java.util.*;

/** Places each ego hop level on a centred grid, with successive grids rising in z. */
public final class RadialPyramid3DLayout<U> implements Layout<U> {
    private final U root;
    private final EdgeOrientation direction;
    private final double spacing, zScale;

    public RadialPyramid3DLayout(U root, EdgeOrientation direction, double spacing, double zScale) {
        this.root=root;this.direction=Objects.requireNonNull(direction);
        this.spacing=AbstractStaticLayout.positive(spacing,"spacing");
        this.zScale=AbstractStaticLayout.positive(zScale,"zScale");
    }

    @Override public LayoutDescriptor getDescriptor() { return new LayoutDescriptor("radial-pyramid-3d","Grid / ego (3D)",3); }

    @Override public LayoutResult<U> compute(Graph<U> graph, LayoutRequest<U> request) {
        IndexedGraphSnapshot<U> snapshot=IndexedGraphSnapshot.capture(graph,request);
        List<U> nodes=snapshot.getNodes();int count=nodes.size(),start=root==null?0:snapshot.indexOf(root);
        if(start<0||(root!=null&&count==0)) throw new IllegalArgumentException("Pyramid ego root must exist in the graph");
        int[] level=new int[count];Arrays.fill(level,-1);
        if(count>0) {
            int[][] adjacent=Topology.neighbours(snapshot,direction);Deque<Integer> queue=new ArrayDeque<>();
            level[start]=0;queue.add(start);
            while(!queue.isEmpty()) { int node=queue.remove();for(int neighbour:adjacent[node]) if(level[neighbour]<0) {
                level[neighbour]=level[node]+1;queue.add(neighbour);
            } }
        }
        int max=0;for(int value:level)max=Math.max(max,value);
        List<List<U>> grids=new ArrayList<>();for(int i=0;i<=max+1;i++)grids.add(new ArrayList<>());
        Map<U,Integer> nodeLevels=new HashMap<>();
        for(int i=0;i<count;i++) { U node=nodes.get(i);nodeLevels.put(node,level[i]);grids.get(level[i]<0?max+1:level[i]).add(node); }
        Map<U,Point3D> positions=new LinkedHashMap<>();
        for(int z=0;z<grids.size();z++) {
            List<U> grid=grids.get(z);int columns=(int)Math.ceil(Math.sqrt(grid.size()));
            int rows=columns==0?0:(int)Math.ceil((double)grid.size()/columns);
            for(int i=0;i<grid.size();i++) {
                int row=i/columns,column=i%columns;
                int rowCount=Math.min(columns,grid.size()-row*columns);
                double x=(column-(rowCount-1)/2.0)*spacing;
                double y=((rows-1)/2.0-row)*spacing;
                int hop=nodeLevels.get(grid.get(i));double height=hop<0?-1:hop;
                positions.put(grid.get(i),new Point3D(x,y,height*zScale));
            }
        }
        for(U pin:request.getPinnedNodes()) {
            Point3D point=request.getInitialPositions3D().get(pin);
            if(point==null)throw new IllegalArgumentException("Spatial pins require x/y/z coordinates");
            positions.put(pin,point);
        }
        return LayoutResult.spatial(positions,new LayoutDiagnostics(getDescriptor().getId(),LayoutDiagnostics.Termination.COMPLETED));
    }
}
