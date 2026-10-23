/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.gui;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.graph.attributes.AttributeType;
import es.uam.eps.ir.relison.io.graph.GexfGraphReader;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;
import org.ranksys.formats.parsing.Parsers;

public class GraphMultiplicityTest {
    private Graph<String> read(String direction, String edges) {
        String xml = "<gexf xmlns=\"http://www.gexf.net/1.2draft\"><graph defaultedgetype=\"" + direction
            + "\"><nodes><node id=\"a\" label=\"Alpha\"/><node id=\"b\"/></nodes><edges>" + edges + "</edges></graph></gexf>";
        Graph<String> graph = new GexfGraphReader<String>(true, true, true, true, Parsers.sp)
            .read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), true, false);
        assertNotNull(graph); return graph;
    }
    @Test public void noParallelEdgesBecomeASimpleFastGraphWithoutLosingData() throws Exception {
        Graph<String> source = read("directed", "<edge source=\"a\" target=\"b\" weight=\"2.5\" label=\"AB\"/><edge source=\"a\" target=\"a\" weight=\"3\"/>");
        source.defineNodeAttribute("empty", AttributeType.STRING);
        Graph<String> graph = GraphController.inferGraphMultiplicity(source);
        assertFalse(graph.isMultigraph()); assertTrue(graph instanceof FastGraph);
        assertTrue(graph.isDirected()); assertTrue(graph.isWeighted());
        assertEquals(2, graph.getVertexCount()); assertEquals(2, graph.getEdgeCount());
        assertEquals(2.5, graph.getEdgeWeight("a", "b"), 0);
        assertEquals(3, graph.getEdgeWeight("a", "a"), 0);
        assertEquals("Alpha", graph.getNodeAttribute("a", "label"));
        assertEquals("AB", graph.getEdgeAttribute("a", "b", "label"));
        assertEquals(AttributeType.STRING, graph.getNodeAttributeType("empty"));
    }
    @Test public void directedReciprocalEdgesAreNotParallel() throws Exception {
        Graph<String> graph = GraphController.inferGraphMultiplicity(read("directed", "<edge source=\"a\" target=\"b\"/><edge source=\"b\" target=\"a\"/>"));
        assertFalse(graph.isMultigraph()); assertEquals(2, graph.getEdgeCount());
    }
    @Test public void trueParallelEdgesAndRepeatedLoopsRetainMultigraph() throws Exception {
        for (String edges : new String[] {
            "<edge source=\"a\" target=\"b\"/><edge source=\"a\" target=\"b\"/>",
            "<edge source=\"a\" target=\"a\"/><edge source=\"a\" target=\"a\"/>" }) {
            Graph<String> source = read("directed", edges);
            assertSame(source, GraphController.inferGraphMultiplicity(source)); assertEquals(2, source.getEdgeCount());
        }
    }
    @Test public void undirectedPairsDetectReverseDuplicatesAndRetainSingleEdgesOnce() throws Exception {
        Graph<String> single = GraphController.inferGraphMultiplicity(read("undirected", "<edge source=\"a\" target=\"b\"/>"));
        assertFalse(single.isMultigraph()); assertEquals(1, single.getEdgeCount());
        Graph<String> parallel = read("undirected", "<edge source=\"a\" target=\"b\"/><edge source=\"b\" target=\"a\"/>");
        assertSame(parallel, GraphController.inferGraphMultiplicity(parallel));
    }
    @Test public void edgelessGraphsBecomeSimpleAndKeepIsolatedNodes() throws Exception {
        Graph<String> graph = GraphController.inferGraphMultiplicity(read("directed", ""));
        assertFalse(graph.isMultigraph()); assertEquals(2, graph.getVertexCount()); assertEquals(0, graph.getEdgeCount());
    }
}
