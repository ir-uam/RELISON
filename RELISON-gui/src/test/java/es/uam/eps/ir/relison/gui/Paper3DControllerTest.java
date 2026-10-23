package es.uam.eps.ir.relison.gui;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.fast.FastDirectedUnweightedGraph;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class Paper3DControllerTest {
    @Test public void gridAcceptsColumnAndDepthCountsAndRejectsFractionalCounts() {
        var response=LayoutController.compute(graph(),Map.of("algorithm","grid-3d","params",Map.of("columns",1,"depth",1)));
        Map<?,?> p=(Map<?,?>)response.get("positions");for(Object point:p.values())assertEquals(0.0,((Map<?,?>)point).get("z"));
        assertThrows(IllegalArgumentException.class,()->LayoutController.compute(graph(),Map.of("algorithm","grid-3d","params",Map.of("depth",1.5))));
        assertThrows(IllegalArgumentException.class,()->LayoutController.compute(graph(),Map.of("algorithm","grid-3d","params",Map.of("columns",-1))));
    }
    @Test public void spherePackingCanRunWithoutAGrouping() {
        var result=LayoutController.compute(graph(),Map.of("algorithm","sphere-packing-3d"));
        assertEquals(3,result.get("dimensions"));assertEquals(3,((Map<?,?>)result.get("positions")).size());
    }
    private Graph<String> graph() { Graph<String> g=new FastDirectedUnweightedGraph<>();g.addEdge("a","b");g.addEdge("a","c");return g; }
    @Test public void apiComputesAllThreePaperModelsWithoutChangingTopology() {
        for(String algorithm:List.of("spherical-mds-3d","upward-grid-3d","h3-3d")) {
            Graph<String> g=graph();var body=Map.of("algorithm",algorithm,"params",Map.of("seed",42,"iterations",4));
            var result=LayoutController.compute(g,body);assertEquals(3,result.get("dimensions"));
            assertEquals(Set.of("a","b","c"),((Map<?,?>)result.get("positions")).keySet());
            assertEquals(3,g.getVertexCount());assertEquals(2,g.getEdgeCount());
            assertThrows(IllegalArgumentException.class,()->LayoutController.compute(g,Map.of("algorithm",algorithm,"params",Map.of("removeOverlap",true))));
        }
    }
    @Test public void sphericalSessionMatchesBatch() {
        var body=Map.of("algorithm","spherical-mds-3d","params",Map.of("seed",17,"iterations",5));Graph<String> g=graph();
        var batch=LayoutController.compute(g,body);var controller=new AnimatedLayoutController(new GraphStore());
        var first=controller.start(g,body);var last=controller.advance((String)first.get("sessionId"),5);
        assertEquals(batch.get("positions"),last.get("positions"));assertEquals(true,last.get("finished"));
    }
    @Test public void h3UsesSuppliedTreeAndRejectsUnknownParents() {
        var r=LayoutController.compute(graph(),Map.of("algorithm","h3-3d","parents",Map.of("b","a","c","a")));
        assertEquals(3,r.get("dimensions"));
        assertThrows(IllegalArgumentException.class,()->LayoutController.compute(graph(),Map.of("algorithm","h3-3d","parents",Map.of("b","missing"))));
    }
    @Test public void spectralSupportsBothDimensionsAndReportsItsBudget() {
        for(String algorithm:List.of("spectral","spectral-3d")) {
            var result=LayoutController.compute(graph(),Map.of("algorithm",algorithm,"params",Map.of("normalized",false,"scale",10,"iterations",500)));
            Map<?,?> point=(Map<?,?>)((Map<?,?>)result.get("positions")).get("a");
            assertEquals(algorithm.endsWith("-3d")?Set.of("x","y","z"):Set.of("x","y"),point.keySet());
            assertEquals("CONVERGED",result.get("termination"));
        }
    }
}
