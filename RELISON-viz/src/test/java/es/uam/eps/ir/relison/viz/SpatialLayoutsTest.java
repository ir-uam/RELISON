/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.fast.*;
import es.uam.eps.ir.relison.viz.layouts.*;
import es.uam.eps.ir.relison.viz.transforms.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import static org.junit.Assert.*;

public class SpatialLayoutsTest {
    private Graph<Integer> graph(int n) {
        Graph<Integer> g=new FastUndirectedUnweightedGraph<>();for(int i=0;i<n;i++) g.addNode(i);return g;
    }
    private double distance(Point3D a,Point3D b) {
        return Math.hypot(Math.hypot(a.getX()-b.getX(),a.getY()-b.getY()),a.getZ()-b.getZ());
    }
    private List<IterativeLayout<Integer>> iterative() {
        return Arrays.asList(new FruchtermanReingold3DLayout<>(),new StressMajorization3DLayout<>());
    }
    @Test public void staticLayoutsHaveDepthBoundsAndDeterministicCoverage() {
        Graph<Integer> g=graph(12);
        LayoutRequest<Integer> request=LayoutRequest.<Integer>builder().seed(42).build();
        for(Layout<Integer> layout:Arrays.<Layout<Integer>>asList(new Random3DLayout<>(),new Grid3DLayout<>(),new SphericalLayout<>())) {
            LayoutResult<Integer> result=layout.compute(g,request);
            assertEquals(3,layout.getDescriptor().getSpaceDimensions());assertEquals(3,result.getSpaceDimensions());
            assertEquals(12,result.getPositions3D().size());
            assertEquals(result.getPositions3D(),layout.compute(g,request).getPositions3D());
            Bounds3D bounds=result.getBounds3D().orElseThrow();assertTrue(bounds.getMax().getZ()>bounds.getMin().getZ());
            assertThrows(UnsupportedOperationException.class,()->result.getPositions3D().clear());
            result.getPositions3D().forEach((id,p)->assertEquals(p.getX(),result.getPositions().get(id).v1(),0));
        }
        LayoutResult<Integer> sphere=new SphericalLayout<Integer>(10).compute(g);
        for(Point3D p:sphere.getPositions3D().values()) assertEquals(10,distance(p,new Point3D(0,0,0)),1e-10);
    }
    @Test public void frForcesUseTheZAxisAndTemperatureCap() {
        Graph<Integer> g=graph(2);g.addEdge(0,1);
        LayoutRequest<Integer> request=LayoutRequest.<Integer>builder().maxIterations(1)
            .initialPositions3D(Map.of(0,new Point3D(0,0,0),1,new Point3D(0,0,20))).build();
        LayoutResult<Integer> result=new FruchtermanReingold3DLayout<Integer>(new FruchtermanReingold3DConfig(10,.95,0,false)).compute(g,request);
        assertEquals(10,result.getPositions3D().get(0).getZ(),1e-10);
        assertEquals(10,result.getPositions3D().get(1).getZ(),1e-10);
        assertEquals(0,result.getPositions3D().get(0).getX(),0);
        assertEquals(10,result.getDiagnostics().getMaximumDisplacement(),1e-10);
    }
    @Test public void iterativeLayoutsPreservePinsAndMatchBatch() {
        Graph<Integer> g=graph(5);g.addEdge(0,1);g.addEdge(1,2);g.addEdge(2,3);
        Point3D pin=new Point3D(10,20,30);
        LayoutRequest<Integer> request=LayoutRequest.<Integer>builder().seed(42).maxIterations(8)
            .initialPositions3D(Map.of(0,pin)).pinnedNodes(Set.of(0)).build();
        for(IterativeLayout<Integer> layout:iterative()) {
            LayoutSession<Integer> a=layout.initialize(g,request),b=layout.initialize(g,request);
            Map<Integer,Point3D> initial=b.snapshot().getPositions3D();
            a.step(3);a.step(5);
            assertEquals(layout.compute(g,request).getPositions3D(),a.snapshot().getPositions3D());
            assertEquals(initial,b.snapshot().getPositions3D());
            assertEquals(pin,a.snapshot().getPositions3D().get(0));
            assertEquals(3,a.snapshot().getSpaceDimensions());
        }
    }
    @Test public void spatialStressDecreasesAndFindsATetrahedron() {
        Graph<Integer> g=graph(4);for(int i=0;i<4;i++) for(int j=i+1;j<4;j++) g.addEdge(i,j);
        LayoutSession<Integer> session=new StressMajorization3DLayout<Integer>(new StressMajorizationConfig(50,1e-10,false))
            .initialize(g,LayoutRequest.<Integer>builder().seed(12).maxIterations(200).build());
        double last=Double.POSITIVE_INFINITY;
        while(!session.isFinished()) {
            session.step(1);Map<Integer,Point3D> positions=session.snapshot().getPositions3D();double stress=0;
            for(int i=0;i<4;i++) for(int j=i+1;j<4;j++) stress+=Math.pow((distance(positions.get(i),positions.get(j))-50)/50,2);
            assertTrue(stress<=last+1e-9);last=stress;
        }
        assertTrue(last<1e-6);
    }
    @Test public void cancellationAndZeroIterationSnapshotsRemainSpatial() {
        Graph<Integer> g=graph(6);g.addEdge(0,1);
        for(IterativeLayout<Integer> layout:iterative()) {
            AtomicBoolean cancelled=new AtomicBoolean();
            LayoutSession<Integer> session=layout.initialize(g,LayoutRequest.<Integer>builder().cancellation(cancelled::get).build());
            Map<Integer,Point3D> before=session.snapshot().getPositions3D();cancelled.set(true);session.step(10);
            assertEquals(LayoutDiagnostics.Termination.CANCELLED,session.snapshot().getDiagnostics().getTermination());
            assertEquals(before,session.snapshot().getPositions3D());
            LayoutResult<Integer> zero=layout.compute(g,LayoutRequest.<Integer>builder().maxIterations(0).build());
            assertEquals(0,zero.getDiagnostics().getIterations());assertEquals(6,zero.getPositions3D().size());
            assertEquals(3,layout.compute(graph(0)).getSpaceDimensions());
        }
    }
    @Test public void overlapRemovalSeparatesSpheresAndKeepsPins() {
        Point3D origin=new Point3D(0,0,0);
        LayoutRequest<Integer> request=LayoutRequest.<Integer>builder().initialPositions3D(Map.of(0,origin,1,origin))
            .pinnedNodes(Set.of(0)).nodeSizes(Map.of(0,2.0,1,3.0)).build();
        Layout<Integer> layout=new LayoutPipeline<>(new Preset3DLayout<>(),List.of(new OverlapRemoval3D<>(1,100)));
        LayoutResult<Integer> result=layout.compute(graph(2),request);
        assertEquals(origin,result.getPositions3D().get(0));
        assertTrue(distance(result.getPositions3D().get(0),result.getPositions3D().get(1))>=6);
        assertThrows(IllegalArgumentException.class,()->new OverlapRemoval<Integer>().process(result,request));
        assertThrows(IllegalArgumentException.class,()->new LayoutPipeline<>(new Preset3DLayout<Integer>(),List.of(new CoordinateTransform<Integer>(1,1,0,0))).compute(graph(2),request));
    }
    @Test public void invalidSpatialInputsAndUnknownNodesAreRejected() {
        assertThrows(IllegalArgumentException.class,()->new Point3D(0,0,Double.NaN));
        assertThrows(IllegalArgumentException.class,()->new Preset3DLayout<Integer>().compute(graph(1)));
        LayoutRequest<Integer> request=LayoutRequest.<Integer>builder().initialPositions3D(Map.of(8,new Point3D(0,0,0))).build();
        assertThrows(IllegalArgumentException.class,()->new Random3DLayout<Integer>().compute(graph(2),request));
        assertThrows(IllegalArgumentException.class,()->new FruchtermanReingold3DConfig(600,600,Double.POSITIVE_INFINITY,60,0,false));
        LayoutRequest<Integer> outside=LayoutRequest.<Integer>builder().initialPositions3D(Map.of(0,new Point3D(0,0,1000))).pinnedNodes(Set.of(0)).build();
        assertThrows(IllegalArgumentException.class,()->new FruchtermanReingold3DLayout<Integer>().compute(graph(2),outside));
    }
}
