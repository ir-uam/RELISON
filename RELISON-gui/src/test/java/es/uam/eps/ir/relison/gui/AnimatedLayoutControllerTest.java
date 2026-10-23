/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.gui;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.fast.FastDirectedUnweightedGraph;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Session advancement, cleanup, and final postprocessor contract. */
public class AnimatedLayoutControllerTest {
    private Graph<String> graph() {
        Graph<String> graph=new FastDirectedUnweightedGraph<>();
        graph.addEdge("a","b"); graph.addNode("c");
        return graph;
    }
    @Test
    public void sessionsMatchBatchAfterConsecutiveStepsAndAreRemoved() {
        for(String algorithm:List.of("fruchterman-reingold","relison-forceatlas2")) {
            AnimatedLayoutController controller=new AnimatedLayoutController(new GraphStore());
            Map<String,Object> body=Map.of("algorithm",algorithm,"params",Map.of("iterations",12,"seed",42));
            Map<String,Object> frame=controller.start(graph(),body);
            String id=(String)frame.get("sessionId");
            assertEquals(false,frame.get("finished"));
            assertEquals(0,frame.get("iterations"));
            frame=controller.advance(id,3);
            assertEquals(3,frame.get("iterations"));
            frame=controller.advance(id,9);
            assertEquals(true,frame.get("finished"));
            assertEquals("LIMIT_REACHED",frame.get("termination"));
            assertEquals(LayoutController.compute(graph(),body).get("positions"),frame.get("positions"));
            assertThrows(NoSuchElementException.class,()->controller.advance(id,1));
            assertFalse(controller.cancel(id));
        }
    }
    @Test
    public void cancellationRemovesOnlyTheSelectedSession() {
        AnimatedLayoutController controller=new AnimatedLayoutController(new GraphStore());
        Map<String,Object> body=Map.of("algorithm","fruchterman-reingold");
        String a=(String)controller.start(graph(),body).get("sessionId");
        String b=(String)controller.start(graph(),body).get("sessionId");
        assertTrue(controller.cancel(a)); assertFalse(controller.cancel(a));
        assertThrows(NoSuchElementException.class,()->controller.advance(a,1));
        assertEquals(1,controller.advance(b,1).get("iterations"));
        controller.cancel(b);
    }
    @Test
    public void invalidRequestsAndStaticAlgorithmsAreRejected() {
        AnimatedLayoutController controller=new AnimatedLayoutController(new GraphStore());
        assertThrows(IllegalArgumentException.class,()->controller.start(graph(),Map.of("algorithm","grid")));
        String id=(String)controller.start(graph(),Map.of("algorithm","relison-forceatlas2")).get("sessionId");
        assertThrows(IllegalArgumentException.class,()->controller.advance(id,0));
        assertThrows(IllegalArgumentException.class,()->controller.advance(id,11));
        assertTrue(controller.cancel(id));
    }
    @Test
    public void finalPackingAndOverlapRemovalMatchBatch() {
        AnimatedLayoutController controller=new AnimatedLayoutController(new GraphStore());
        Map<String,Object> body=Map.of("algorithm","fruchterman-reingold",
            "params",Map.of("iterations",5,"removeOverlap",true,"overlapGap",2,"packComponents",true,"packingGap",10),
            "nodeSizes",Map.of("a",3,"b",3,"c",3));
        Map<String,Object> frame=controller.start(graph(),body);
        String id=(String)frame.get("sessionId");
        frame=controller.advance(id,5);
        assertEquals(true,frame.get("finished"));
        assertEquals(LayoutController.compute(graph(),body).get("positions"),frame.get("positions"));
        Map<String,Object> zero=controller.start(graph(),Map.of("algorithm","relison-forceatlas2","params",Map.of("iterations",0)));
        assertEquals(true,zero.get("finished"));
        assertThrows(NoSuchElementException.class,()->controller.advance((String)zero.get("sessionId"),1));
    }
}
