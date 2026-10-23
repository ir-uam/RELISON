/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.fast.FastDirectedUnweightedGraph;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.viz.layouts.RadialPyramid3DLayout;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class GridEgo3DTest {
    @Test public void requestOrderControlsPlacementWithinLayersWithoutChangingHops() {
        Graph<String> graph = new FastDirectedUnweightedGraph<>();
        for(String node:Arrays.asList("root","a","b","c","isolated"))graph.addNode(node);
        for(String node:Arrays.asList("a","b","c"))graph.addEdge("root",node);
        RadialPyramid3DLayout<String> layout = new RadialPyramid3DLayout<>("root",EdgeOrientation.OUT,10,20);
        LayoutResult<String> forward=layout.compute(graph,LayoutRequest.<String>builder().nodeOrder(Arrays.asList("root","a","b","c","isolated")).build());
        LayoutResult<String> reverse=layout.compute(graph,LayoutRequest.<String>builder().nodeOrder(Arrays.asList("isolated","c","b","a","root")).build());
        assertEquals(forward.getPositions3D().get("a"),reverse.getPositions3D().get("c"));
        assertEquals(forward.getPositions3D().get("c"),reverse.getPositions3D().get("a"));
        assertEquals(forward.getPositions3D().get("root"),reverse.getPositions3D().get("root"));
        assertEquals(20,reverse.getPositions3D().get("a").getZ(),0);
        assertEquals(-20,reverse.getPositions3D().get("isolated").getZ(),0);
    }
}
