package es.uam.eps.ir.relison.viz;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.fast.FastDirectedUnweightedGraph;
import es.uam.eps.ir.relison.graph.fast.FastUndirectedUnweightedGraph;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.viz.layouts.*;
import es.uam.eps.ir.relison.viz.internal.SphericalMdsSession;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class Paper3DLayoutsTest {
    private Graph<Integer> graph(int n,boolean directed) {
        Graph<Integer> g=directed?new FastDirectedUnweightedGraph<>():new FastUndirectedUnweightedGraph<>();
        for(int i=0;i<n;i++)g.addNode(i);return g;
    }
    private LayoutRequest<Integer> request(int n) {
        List<Integer> order=new ArrayList<>();for(int i=0;i<n;i++)order.add(i);
        return LayoutRequest.<Integer>builder().nodeOrder(order).seed(42).maxIterations(40).build();
    }
    private double norm(Point3D p) { return Math.hypot(Math.hypot(p.getX(),p.getY()),p.getZ()); }
    @Test public void upwardMatchesTheorem31AndRejectsCycles() {
        Graph<Integer> g=graph(5,true);for(int i=0;i<4;i++)g.addEdge(i,i+1);
        var p=new UpwardGrid3DLayout<Integer>(1).compute(g,request(5)).getPositions3D();
        for(int i=1;i<=5;i++) { assertEquals((i*i*i)%7,p.get(i-1).getX(),0);assertEquals((i*i)%7,p.get(i-1).getY(),0);assertEquals(i,p.get(i-1).getZ(),0); }
        g.addEdge(4,0);assertThrows(IllegalArgumentException.class,()->new UpwardGrid3DLayout<Integer>().compute(g,request(5)));
        assertThrows(IllegalArgumentException.class,()->new UpwardGrid3DLayout<Integer>().compute(graph(2,false),request(2)));
    }
    @Test public void upwardCompleteDagHasNoCoplanarFourVertices() {
        Graph<Integer> g=graph(9,true);for(int i=0;i<9;i++)for(int j=i+1;j<9;j++)g.addEdge(i,j);
        var p=new UpwardGrid3DLayout<Integer>().compute(g,request(9)).getPositions3D();
        for(int a=0;a<9;a++)for(int b=a+1;b<9;b++)for(int c=b+1;c<9;c++)for(int d=c+1;d<9;d++) {
            Point3D o=p.get(a),u=p.get(b),v=p.get(c),w=p.get(d);
            double ux=u.getX()-o.getX(),uy=u.getY()-o.getY(),uz=u.getZ()-o.getZ();
            double vx=v.getX()-o.getX(),vy=v.getY()-o.getY(),vz=v.getZ()-o.getZ();
            double wx=w.getX()-o.getX(),wy=w.getY()-o.getY(),wz=w.getZ()-o.getZ();
            assertTrue(Math.abs(ux*(vy*wz-vz*wy)-uy*(vx*wz-vz*wx)+uz*(vx*wy-vy*wx))>.5);
        }
    }
    @Test public void sphericalGradientMatchesFiniteDifferences() {
        double[] q={.3,-.4,-.2,1.1};double target=.8,h=1e-6;
        double[] actual=SphericalMdsSession.pairGradient(q[0],q[1],q[2],q[3],target);
        for(int k=0;k<4;k++) { double[] plus=q.clone(),minus=q.clone();plus[k]+=h;minus[k]-=h;
            assertEquals((pairStress(plus,target)-pairStress(minus,target))/(2*h),actual[k],1e-6); }
    }
    private double pairStress(double[] q,double target) {
        double angle=Math.acos(Math.sin(q[0])*Math.sin(q[2])+Math.cos(q[0])*Math.cos(q[2])*Math.cos(q[1]-q[3]));return Math.pow(angle-target,2);
    }
    @Test public void sphericalSessionsMatchBatchAndStayOnSphere() {
        Graph<Integer> g=graph(8,false);for(int i=0;i<8;i++)g.addEdge(i,(i+1)%8);
        var layout=new SphericalMdsLayout<Integer>(new SphericalMdsConfig(17,.1,0,false));
        var r=request(8);var session=layout.initialize(g,r);session.step(7);session.step(33);
        assertEquals(layout.compute(g,r).getPositions3D(),session.snapshot().getPositions3D());
        for(Point3D p:session.snapshot().getPositions3D().values())assertEquals(17,norm(p),1e-10);
        assertEquals(40,session.snapshot().getDiagnostics().getIterations());
    }
    @Test public void sphericalPinsDisconnectedAndCancellation() {
        Graph<Integer> g=graph(3,false);g.addEdge(0,1);g.addEdge(1,2);
        Point3D pin=new Point3D(0,0,100);
        var r=LayoutRequest.<Integer>builder().seed(1).maxIterations(5).initialPositions3D(Map.of(0,pin)).pinnedNodes(Set.of(0)).build();
        assertEquals(pin,new SphericalMdsLayout<Integer>().compute(g,r).getPositions3D().get(0));
        assertThrows(IllegalArgumentException.class,()->new SphericalMdsLayout<Integer>().compute(graph(2,false),request(2)));
        var session=new SphericalMdsLayout<Integer>().initialize(g,request(3));var initial=session.snapshot().getPositions3D();session.cancel();session.step(5);
        assertEquals(LayoutDiagnostics.Termination.CANCELLED,session.snapshot().getDiagnostics().getTermination());assertEquals(initial,session.snapshot().getPositions3D());
    }
    @Test public void h3StarMatchesAppendixRadiusAndHemisphere() {
        for(int n:new int[]{2,10,101,501}) {
            Graph<Integer> g=graph(n,true);for(int i=1;i<n;i++)g.addEdge(0,i);
            var layout=new H3Layout<Integer>(0,EdgeOrientation.OUT,100,.1,7.2,Collections.emptyMap());
            var p=layout.compute(g,request(n)).getPositions3D();
            double target=Math.tanh(Math.log(Math.sqrt(7.2*(n-1)*(Math.cosh(.1)-1))+Math.sqrt(7.2*(n-1)*(Math.cosh(.1)-1)+1)))*100;
            assertEquals(0,norm(p.get(0)),0);
            for(int i=1;i<n;i++) { assertEquals(target,norm(p.get(i)),1e-9);assertTrue(p.get(i).getZ()>=0);assertTrue(norm(p.get(i))<100); }
            assertEquals(p,layout.compute(g,request(n)).getPositions3D());
        }
    }
    @Test public void h3NonTreeLinksDoNotChangeSuppliedHierarchy() {
        Graph<Integer> g=graph(5,true);for(int i=1;i<5;i++)g.addEdge((i-1)/2,i);
        var layout=new H3Layout<Integer>(0,EdgeOrientation.OUT,100,.1,7.2,Map.of(1,0,2,0,3,1,4,1));
        var before=layout.compute(g,request(5)).getPositions3D();g.addEdge(3,4);g.addEdge(4,0);
        assertEquals(before,layout.compute(g,request(5)).getPositions3D());
        assertThrows(IllegalArgumentException.class,()->new H3Layout<Integer>(null,EdgeOrientation.OUT,100,.1,7.2,Map.of(0,4,4,0)).compute(g,request(5)));
    }
    @Test public void emptyAndSingletonRemainSpatial() {
        for(Layout<Integer> layout:Arrays.<Layout<Integer>>asList(new UpwardGrid3DLayout<>(),new SphericalMdsLayout<>(),new H3Layout<>())) {
            for(int n=0;n<2;n++) {var r=layout.compute(graph(n,true),request(n));assertEquals(3,r.getSpaceDimensions());assertEquals(n,r.getPositions3D().size());}
        }
    }
}
