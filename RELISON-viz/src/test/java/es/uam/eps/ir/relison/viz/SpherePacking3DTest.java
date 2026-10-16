package es.uam.eps.ir.relison.viz;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.fast.FastUndirectedUnweightedGraph;
import es.uam.eps.ir.relison.viz.layouts.SpherePacking3DLayout;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SpherePacking3DTest {
    private Graph<Integer> graph(int n){Graph<Integer> g=new FastUndirectedUnweightedGraph<>();for(int i=0;i<n;i++)g.addNode(i);return g;}
    private double distance(Point3D a,Point3D b){return Math.hypot(Math.hypot(a.getX()-b.getX(),a.getY()-b.getY()),a.getZ()-b.getZ());}
    @Test public void ungroupedPacksOneCenteredFilledBall() {
        var layout=new SpherePacking3DLayout<Integer>(2);var r=layout.compute(graph(60));
        assertEquals(60,r.getPositions3D().size());double x=0,y=0,z=0,min=Double.POSITIVE_INFINITY,max=0;
        for(Point3D p:r.getPositions3D().values()){x+=p.getX();y+=p.getY();z+=p.getZ();double d=distance(p,new Point3D(0,0,0));min=Math.min(min,d);max=Math.max(max,d);}
        assertEquals(0,x,1e-9);assertEquals(0,y,1e-9);assertEquals(0,z,1e-9);assertTrue(min<max*.5);
        assertTrue(r.getPositions3D().values().stream().anyMatch(p->Math.abs(p.getZ())>1));
        assertEquals(r.getPositions3D(),layout.compute(graph(60)).getPositions3D());
    }
    @Test public void singletonGroupsAreDistributedInThreeDimensions() {
        List<List<Integer>> groups=new ArrayList<>();for(int i=0;i<8;i++)groups.add(List.of(i));
        var p=new SpherePacking3DLayout<Integer>(groups,3).compute(graph(8)).getPositions3D();
        double minY=Double.POSITIVE_INFINITY,maxY=-Double.MAX_VALUE,minZ=minY,maxZ=maxY;
        for(Point3D point:p.values()){minY=Math.min(minY,point.getY());maxY=Math.max(maxY,point.getY());minZ=Math.min(minZ,point.getZ());maxZ=Math.max(maxZ,point.getZ());}
        assertTrue(maxY-minY>1);assertTrue(maxZ-minZ>1);
        for(int i=0;i<8;i++)for(int j=0;j<i;j++)assertTrue(distance(p.get(i),p.get(j))>=3-1e-9);
    }
    @Test public void unequalClustersAndNodeRadiiMaintainSurfaceGaps() {
        List<List<Integer>> groups=List.of(List.of(0),List.of(1,2,3,4,5,6,7,8,9),List.of(10,11),List.of(12,13,14));
        Map<Integer,Double> radii=new HashMap<>();for(int i=0;i<15;i++)radii.put(i,i==6?12.0:1.0+i%3);
        var request=LayoutRequest.<Integer>builder().nodeSizes(radii).build();
        var p=new SpherePacking3DLayout<Integer>(groups,4).compute(graph(15),request).getPositions3D();
        for(int i=0;i<15;i++)for(int j=0;j<i;j++)assertTrue("Nodes "+i+" and "+j,distance(p.get(i),p.get(j))>=radii.get(i)+radii.get(j)+4-1e-8);
    }
    @Test public void emptyAndSingletonAndInvalidGroupsHaveDefinedBehavior() {
        assertTrue(new SpherePacking3DLayout<Integer>(0).compute(graph(0)).getPositions3D().isEmpty());
        assertEquals(new Point3D(0,0,0),new SpherePacking3DLayout<Integer>(0).compute(graph(1)).getPositions3D().get(0));
        assertThrows(IllegalArgumentException.class,()->new SpherePacking3DLayout<Integer>(List.of(List.of(0)),1).compute(graph(2)));
        assertThrows(IllegalArgumentException.class,()->new SpherePacking3DLayout<Integer>(List.of(List.of(0,0)),1));
    }
}
