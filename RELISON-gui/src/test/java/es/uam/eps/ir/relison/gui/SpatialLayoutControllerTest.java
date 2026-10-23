/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.gui;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.fast.FastUndirectedUnweightedGraph;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SpatialLayoutControllerTest {
    private Graph<String> graph() {
        Graph<String> g=new FastUndirectedUnweightedGraph<>();g.addEdge("a","b");g.addNode("c");return g;
    }
    @Test public void batchAndAnimatedResponsesKeepDepthAndAgree() {
        for(String algorithm:List.of("fruchterman-reingold-3d","stress-majorization-3d")) {
            Map<String,Object> body=Map.of("algorithm",algorithm,"params",Map.of("seed",42,"iterations",8));
            Graph<String> g=graph();
            Map<String,Object> batch=LayoutController.compute(g,body);
            assertEquals(3,batch.get("dimensions"));
            AnimatedLayoutController controller=new AnimatedLayoutController(new GraphStore());
            Map<String,Object> frame=controller.start(g,body);String id=(String)frame.get("sessionId");
            frame=controller.advance(id,8);
            assertEquals(true,frame.get("finished"));assertEquals(batch.get("positions"),frame.get("positions"));
            assertEquals(3,frame.get("dimensions"));assertEquals(3,g.getVertexCount());assertEquals(1,g.getEdgeCount());
        }
    }
    @Test public void staticResponsesAndOverlapRemovalReturnXYZ() {
        for(String algorithm:List.of("random-3d","grid-3d","spherical-3d")) {
            Map<String,Object> result=LayoutController.compute(graph(),Map.of("algorithm",algorithm));
            Map<?,?> positions=(Map<?,?>)result.get("positions");Map<?,?> p=(Map<?,?>)positions.get("a");
            assertEquals(Set.of("x","y","z"),p.keySet());
            assertTrue(((Map<?,?>)result.get("bounds")).containsKey("minZ"));
        }
        Map<String,Object> positions=Map.of("a",Map.of("x",0,"y",0,"z",0),"b",Map.of("x",0,"y",0,"z",0),"c",Map.of("x",0,"y",0,"z",0));
        Map<String,Object> result=LayoutController.compute(graph(),Map.of("algorithm","preset-3d","positions",positions,
            "nodeSizes",Map.of("a",2,"b",2,"c",2),"params",Map.of("removeOverlap",true,"overlapGap",1)));
        assertNotEquals(positions,result.get("positions"));assertEquals(3,result.get("dimensions"));
    }
    @Test public void invalidDimensionsAndInapplicableProcessingAreRejected() {
        assertThrows(IllegalArgumentException.class,()->LayoutController.compute(graph(),Map.of("algorithm","preset-3d",
            "positions",Map.of("a",Map.of("x",0,"y",0)))));
        assertThrows(IllegalArgumentException.class,()->LayoutController.compute(graph(),Map.of("algorithm","fruchterman-reingold-3d","params",Map.of("theta",.8))));
        assertThrows(IllegalArgumentException.class,()->LayoutController.compute(graph(),Map.of("algorithm","random-3d","params",Map.of("packComponents",true))));
    }

    @Test public void layeredVariantsUseTheExistingShellScoreAndRootInputs() {
        Graph<String> g=graph();
        for(String family:List.of("shell","concentric","radial")) for(String variant:List.of("sphere","cone")) {
            Map<String,Object> body=new LinkedHashMap<>();
            body.put("algorithm",family+"-"+variant+"-3d");
            body.put("shells",List.of(List.of("a"),List.of("b","c")));
            body.put("scores",Map.of("a",8,"b",-2,"c",8));
            body.put("params",Map.of("spacing",10,"root","a","direction","OUT"));
            Map<String,Object> result=LayoutController.compute(g,body);
            assertEquals(3,result.get("dimensions"));
            Map<?,?> coordinates=(Map<?,?>)result.get("positions");
            assertEquals(Set.of("a","b","c"),coordinates.keySet());
            if(variant.equals("cone")) {
                double expected=family.equals("shell")?1:family.equals("concentric")?8:-1;
                assertEquals(expected,((Number)((Map<?,?>)coordinates.get("c")).get("z")).doubleValue(),0);
                body.put("algorithm",family);
                assertEquals(((Map<?,?>)((Map<?,?>)LayoutController.compute(g,body).get("positions")).get("b")).get("x"),
                    ((Map<?,?>)coordinates.get("b")).get("x"));
            }
        }
        assertEquals(3,LayoutController.compute(g,Map.of("algorithm","concentric-sphere-3d")).get("dimensions"));
        assertEquals(3,LayoutController.compute(g,Map.of("algorithm","shell-cone-3d")).get("dimensions"));
    }

    @Test public void coneZScaleMultipliesOnlyHeightsAndRejectsInvalidValues() {
        for(String family:List.of("shell","concentric","radial")) {
            Map<String,Object> body=new LinkedHashMap<>();
            body.put("algorithm",family+"-cone-3d");
            body.put("shells",List.of(List.of("a"),List.of("b","c")));
            body.put("scores",Map.of("a",8,"b",-2,"c",8));
            body.put("params",Map.of("root","a","spacing",10));
            Map<?,?> original=(Map<?,?>)LayoutController.compute(graph(),body).get("positions");
            body.put("params",Map.of("root","a","spacing",10,"zScale",50));
            Map<?,?> scaled=(Map<?,?>)LayoutController.compute(graph(),body).get("positions");
            for(String node:List.of("a","b","c")) {
                Map<?,?> a=(Map<?,?>)original.get(node),b=(Map<?,?>)scaled.get(node);
                assertEquals(a.get("x"),b.get("x"));assertEquals(a.get("y"),b.get("y"));
                assertEquals(((Number)a.get("z")).doubleValue()*50,((Number)b.get("z")).doubleValue(),0);
            }
            for(double scale:new double[]{0,-1,Double.NaN,Double.POSITIVE_INFINITY}) {
                body.put("params",Map.of("zScale",scale));
                assertThrows(IllegalArgumentException.class,()->LayoutController.compute(graph(),body));
            }
        }
    }
}
