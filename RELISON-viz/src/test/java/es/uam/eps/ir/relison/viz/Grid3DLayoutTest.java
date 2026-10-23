package es.uam.eps.ir.relison.viz;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.fast.FastUndirectedUnweightedGraph;
import es.uam.eps.ir.relison.viz.layouts.Grid3DLayout;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class Grid3DLayoutTest {
    private Graph<Integer> graph(int n){Graph<Integer> g=new FastUndirectedUnweightedGraph<>();for(int i=0;i<n;i++)g.addNode(i);return g;}
    private LayoutRequest<Integer> request(int n){List<Integer> order=new ArrayList<>();for(int i=0;i<n;i++)order.add(i);return LayoutRequest.<Integer>builder().nodeOrder(order).build();}
    @Test public void explicitColumnsAndDepthCalculateRowsAndPreserveFillOrder(){
        var p=new Grid3DLayout<Integer>(4,2,2).compute(graph(24),request(24)).getPositions3D();
        assertEquals(new Point3D(-3,-2,-1),p.get(0));assertEquals(new Point3D(3,-2,-1),p.get(3));
        assertEquals(new Point3D(-3,0,-1),p.get(4));assertEquals(new Point3D(-3,-2,1),p.get(12));
        assertEquals(4,p.values().stream().map(Point3D::getX).distinct().count());
        assertEquals(3,p.values().stream().map(Point3D::getY).distinct().count());
        assertEquals(2,p.values().stream().map(Point3D::getZ).distinct().count());
        assertEquals(new Point3D(-1,2,1),new Grid3DLayout<Integer>(4,2,2).compute(graph(22),request(22)).getPositions3D().get(21));
    }
    @Test public void automaticDimensionsRemainBackwardCompatible(){
        for(int n:new int[]{0,1,2,8,24,30})assertEquals(new Grid3DLayout<Integer>(3).compute(graph(n),request(n)).getPositions3D(),
            new Grid3DLayout<Integer>(0,0,3).compute(graph(n),request(n)).getPositions3D());
        var p=new Grid3DLayout<Integer>(0,2,2).compute(graph(24),request(24)).getPositions3D();
        assertEquals(p,new Grid3DLayout<Integer>(4,2,2).compute(graph(24),request(24)).getPositions3D());
        var oneLayer=new Grid3DLayout<Integer>(3,1,2).compute(graph(10),request(10)).getPositions3D();
        assertTrue(oneLayer.values().stream().allMatch(q->q.getZ()==0));
    }
    @Test public void invalidCountsAndZeroOrNegativeSpacingAreRejected(){
        assertThrows(IllegalArgumentException.class,()->new Grid3DLayout<Integer>(-1,0,1));
        assertThrows(IllegalArgumentException.class,()->new Grid3DLayout<Integer>(0,-1,1));
        assertThrows(IllegalArgumentException.class,()->new Grid3DLayout<Integer>(0,0,0));
    }
}
