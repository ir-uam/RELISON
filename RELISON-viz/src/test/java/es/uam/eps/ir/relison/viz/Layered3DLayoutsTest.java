/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.fast.*;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.viz.layouts.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

/** Geometry and semantic invariants for spherical and cone variants. */
public class Layered3DLayoutsTest {
    private Graph<Integer> graph(int count) {
        Graph<Integer> g=new FastDirectedUnweightedGraph<>();for(int i=0;i<count;i++) g.addNode(i);return g;
    }
    private double radius(Point3D p) { return Math.hypot(Math.hypot(p.getX(),p.getY()),p.getZ()); }
    private void sameXY(LayoutResult<Integer> flat,LayoutResult<Integer> cone) {
        assertEquals(flat.getPositions(),cone.getPositions());assertEquals(3,cone.getSpaceDimensions());
    }
    private void sameRadii(LayoutResult<Integer> flat,LayoutResult<Integer> sphere) {
        assertEquals(flat.getPositions().keySet(),sphere.getPositions3D().keySet());
        flat.getPositions().forEach((id,p)->assertEquals(Math.hypot(p.v1(),p.v2()),radius(sphere.getPositions3D().get(id)),1e-10));
    }
    @Test public void shellConesRetainTheRingsAndUseZeroBasedGroupHeights() {
        Graph<Integer> g=graph(7);List<List<Integer>> groups=List.of(List.of(0),List.of(1,2,3),List.of(4,5,6));
        LayoutRequest<Integer> request=LayoutRequest.<Integer>builder().nodeOrder(List.of(6,5,4,3,2,1,0)).build();
        LayoutResult<Integer> flat=new ShellLayout<Integer>(groups,10).compute(g,request);
        LayoutResult<Integer> cone=new Shell3DLayout<Integer>(groups,10,Layered3DVariant.CONE).compute(g,request);
        sameXY(flat,cone);
        for(int i=0;i<groups.size();i++) for(int node:groups.get(i)) assertEquals(i,cone.getPositions3D().get(node).getZ(),0);
        assertEquals(List.of(6,5,4,3,2,1,0),new ArrayList<>(cone.getPositions3D().keySet()));
    }
    @Test public void shellSpheresKeepTheirRadiiAndProduceDepth() {
        Graph<Integer> g=graph(7);List<List<Integer>> groups=List.of(List.of(0),List.of(1,2,3),List.of(4,5,6));
        LayoutResult<Integer> sphere=new Shell3DLayout<Integer>(groups,10).compute(g);
        sameRadii(new ShellLayout<Integer>(groups,10).compute(g),sphere);
        assertEquals(new Point3D(0,0,0),sphere.getPositions3D().get(0));
        assertTrue(sphere.getPositions3D().values().stream().anyMatch(p->Math.abs(p.getZ())>1));
        // An outer singleton remains on its shell rather than moving to the origin.
        LayoutResult<Integer> singleton=new Shell3DLayout<Integer>(List.of(List.of(0),List.of(1)),10).compute(graph(2));
        assertEquals(10,radius(singleton.getPositions3D().get(1)),1e-10);
    }
    @Test public void concentricConesUseRawScoresAndReverseOnlyTheRadialOrder() {
        Graph<Integer> g=graph(5);Map<Integer,Double> scores=Map.of(0,9.0,1,7.0,2,7.0,3,-2.0,4,-2.0);
        for(boolean reverse:new boolean[]{false,true}) {
            AtomicInteger calls=new AtomicInteger();
            LayoutResult<Integer> cone=new Concentric3DLayout<Integer>(node->{calls.incrementAndGet();return scores.get(node);},10,reverse,Layered3DVariant.CONE).compute(g);
            assertEquals(5,calls.get());
            sameXY(new ConcentricLayout<Integer>(scores::get,10,reverse).compute(g),cone);
            scores.forEach((id,score)->assertEquals(score,cone.getPositions3D().get(id).getZ(),0));
            LayoutResult<Integer> sphere=new Concentric3DLayout<Integer>(scores::get,10,reverse,Layered3DVariant.SPHERE).compute(g);
            sameRadii(new ConcentricLayout<Integer>(scores::get,10,reverse).compute(g),sphere);
        }
    }
    @Test public void radialVariantsFollowDirectedHopsAndMarkUnreachableNodes() {
        Graph<Integer> g=graph(6);g.addEdge(0,1);g.addEdge(1,2);g.addEdge(3,0);g.addEdge(0,4);g.addEdge(4,0);
        Map<EdgeOrientation,double[]> expected=Map.of(EdgeOrientation.OUT,new double[]{0,1,2,-1,1,-1},
            EdgeOrientation.IN,new double[]{0,-1,-1,1,1,-1},EdgeOrientation.MUTUAL,new double[]{0,-1,-1,-1,1,-1},
            EdgeOrientation.UND,new double[]{0,1,2,1,1,-1});
        for(EdgeOrientation direction:expected.keySet()) {
            LayoutResult<Integer> flat=new RadialLayout<Integer>(0,direction,10).compute(g);
            LayoutResult<Integer> cone=new Radial3DLayout<Integer>(0,direction,10,Layered3DVariant.CONE).compute(g);
            sameXY(flat,cone);
            for(int i=0;i<6;i++) assertEquals(expected.get(direction)[i],cone.getPositions3D().get(i).getZ(),0);
            sameRadii(flat,new Radial3DLayout<Integer>(0,direction,10,Layered3DVariant.SPHERE).compute(g));
        }
    }
    @Test public void pinsOverrideGeometryWithoutChangingOtherNodes() {
        Graph<Integer> g=graph(3);g.addEdge(0,1);g.addEdge(1,2);
        Point3D pin=new Point3D(200,300,400);
        LayoutRequest<Integer> request=LayoutRequest.<Integer>builder().initialPositions3D(Map.of(1,pin)).pinnedNodes(Set.of(1)).build();
        for(Layered3DVariant variant:Layered3DVariant.values()) {
            Layout<Integer> layout=new Radial3DLayout<>(0,EdgeOrientation.UND,10,variant);
            LayoutResult<Integer> result=layout.compute(g,request),unpinned=layout.compute(g);
            assertEquals(pin,result.getPositions3D().get(1));
            assertEquals(unpinned.getPositions3D().get(2),result.getPositions3D().get(2));
        }
    }
    @Test public void validationAndEmptyGraphsFollowExistingContracts() {
        for(Layered3DVariant variant:Layered3DVariant.values()) {
            assertEquals(3,new Shell3DLayout<Integer>(List.of(),10,variant).compute(graph(0)).getSpaceDimensions());
            assertEquals(3,new Concentric3DLayout<Integer>(id->1,10,false,variant).compute(graph(0)).getSpaceDimensions());
            assertEquals(3,new Radial3DLayout<Integer>(null,EdgeOrientation.UND,10,variant).compute(graph(0)).getSpaceDimensions());
            assertThrows(IllegalArgumentException.class,()->new Radial3DLayout<Integer>(99,EdgeOrientation.OUT,10,variant).compute(graph(3)));
            assertThrows(IllegalArgumentException.class,()->new Concentric3DLayout<Integer>(id->Double.NaN,10,false,variant).compute(graph(3)));
            assertThrows(IllegalArgumentException.class,()->new Shell3DLayout<Integer>(List.of(List.of(0)),10,variant).compute(graph(3)));
        }
        assertThrows(IllegalArgumentException.class,()->new Shell3DLayout<Integer>(List.of(List.of(0),List.of(0)),10));
    }

    @Test public void coneHeightMultiplierLeavesXYAndPinsUnchanged() {
        Graph<Integer> g=graph(3);g.addEdge(0,1);g.addEdge(1,2);
        List<List<Integer>> shells=List.of(List.of(0),List.of(1),List.of(2));
        Map<Integer,Double> scores=Map.of(0,-2.0,1,0.0,2,3.0);
        for(Layered3DVariant variant:Layered3DVariant.values()) {
            List<Layout<Integer>> original=List.of(new Shell3DLayout<>(shells,10,variant),
                new Concentric3DLayout<>(scores::get,10,false,variant),new Radial3DLayout<>(0,EdgeOrientation.OUT,10,variant));
            List<Layout<Integer>> scaled=List.of(new Shell3DLayout<>(shells,10,variant,25),
                new Concentric3DLayout<>(scores::get,10,false,variant,25),new Radial3DLayout<>(0,EdgeOrientation.OUT,10,variant,25));
            for(int i=0;i<original.size();i++) {
                LayoutResult<Integer> a=original.get(i).compute(g),b=scaled.get(i).compute(g);
                assertEquals(a.getPositions(),b.getPositions());
                for(int node=0;node<3;node++)
                    assertEquals(a.getPositions3D().get(node).getZ()*(variant==Layered3DVariant.CONE?25:1),b.getPositions3D().get(node).getZ(),1e-10);
                Point3D pin=new Point3D(100,200,300);
                LayoutRequest<Integer> pinned=LayoutRequest.<Integer>builder().initialPositions3D(Map.of(1,pin)).pinnedNodes(Set.of(1)).build();
                assertEquals(pin,scaled.get(i).compute(g,pinned).getPositions3D().get(1));
            }
        }
        for(double scale:new double[]{0,-1,Double.NaN,Double.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class,()->new Shell3DLayout<Integer>(shells,10,Layered3DVariant.CONE,scale));
        assertThrows(IllegalArgumentException.class,()->new Concentric3DLayout<Integer>(id->Double.MAX_VALUE,10,false,Layered3DVariant.CONE,2).compute(g));
    }
}
