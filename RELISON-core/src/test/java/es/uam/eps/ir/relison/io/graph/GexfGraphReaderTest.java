package es.uam.eps.ir.relison.io.graph;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.multigraph.MultiGraph;
import org.junit.Test;
import org.ranksys.formats.parsing.Parsers;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class GexfGraphReaderTest
{
    private Graph<String> read(String body, boolean multi)
    {
        return read(body, multi, "", true);
    }

    private Graph<String> read(String body, boolean multi, String direction, boolean fallback)
    {
        String xml = "<gexf xmlns='http://gexf.net/1.3'><graph " + direction + ">" + body + "</graph></gexf>";
        return new GexfGraphReader<>(multi, fallback, true, false, Parsers.sp)
                .read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    @Test public void importsDefaultsAndOverrides()
    {
        Graph<String> graph = read("<attributes class='node'>"
                + "<attribute id='0' title='score' type='float'><default>2.5</default></attribute>"
                + "</attributes><nodes><node id='a'><attvalues><attvalue for='0' value='4.5'/>"
                + "</attvalues></node></nodes><edges><edge source='a' target='b' weight='3'/></edges>", false);
        assertNotNull(graph);
        assertEquals(4.5, graph.getNodeAttribute("a", "score"));
        assertEquals(2.5, graph.getNodeAttribute("b", "score"));
        assertEquals(3.0, graph.getEdgeWeight("a", "b"), 0.0);
    }

    @Test public void importsEachParallelEdge()
    {
        MultiGraph<String> graph = (MultiGraph<String>) read("<attributes class='edge'>"
                + "<attribute id='0' title='kind' type='string'><default>friend</default></attribute>"
                + "</attributes><edges><edge source='a' target='b'/><edge source='a' target='b'>"
                + "<attvalues><attvalue for='0' value=' colleague '/></attvalues></edge></edges>", true);
        assertNotNull(graph);
        assertEquals(2, graph.getNumEdges("a", "b"));
        assertEquals("friend", graph.getEdgeAttribute("a", "b", 0, "kind"));
        assertEquals(" colleague ", graph.getEdgeAttribute("a", "b", 1, "kind"));
    }

    @Test public void rejectsMalformedTypedValues()
    {
        assertNull(read("<attributes class='node'><attribute id='0' title='score' type='integer'/>"
                + "</attributes><nodes><node id='a'><attvalues><attvalue for='0' value='oops'/>"
                + "</attvalues></node></nodes>", false));
    }

    @Test public void importsBuiltInNodeAndEdgeLabels()
    {
        Graph<String> graph = read("<nodes><node id='a' label='Alice &amp; Bob'/>"
                + "<node id='b'/><node id='c' label=''/></nodes>"
                + "<edges><edge source='a' target='b' label='friend'/></edges>", false);
        assertNotNull(graph);
        assertEquals("Alice & Bob", graph.getNodeAttribute("a", "label"));
        assertNull(graph.getNodeAttribute("b", "label"));
        assertEquals("", graph.getNodeAttribute("c", "label"));
        assertEquals("friend", graph.getEdgeAttribute("a", "b", "label"));
    }

    @Test public void importsParallelEdgeLabelsIndividually()
    {
        MultiGraph<String> graph = (MultiGraph<String>) read("<edges>"
                + "<edge source='a' target='b' label='friend'/>"
                + "<edge source='a' target='b' label='colleague'/></edges>", true);
        assertNotNull(graph);
        assertEquals("friend", graph.getEdgeAttribute("a", "b", 0, "label"));
        assertEquals("colleague", graph.getEdgeAttribute("a", "b", 1, "label"));
    }

    @Test public void importsVisualizationWithAlternativePrefix()
    {
        Graph<String> graph = read("<nodes><node id='a' xmlns:v='http://www.gexf.net/1.2draft/viz'>"
                + "<v:position x='-12.5' y='30' z='2'/><v:size value='8.5'/></node>"
                + "<node id='b' xmlns:v='http://gexf.net/1.3/viz'><v:position x='NaN' y='oops'/>"
                + "<v:size value='-1'/></node><node id='c'><size value='99'/></node></nodes>", false);
        assertNotNull(graph);
        assertEquals(-12.5, graph.getNodeAttribute("a", "viz:x"));
        assertEquals(30.0, graph.getNodeAttribute("a", "viz:y"));
        assertEquals(2.0, graph.getNodeAttribute("a", "viz:z"));
        assertEquals(8.5, graph.getNodeAttribute("a", "viz:size"));
        assertNull(graph.getNodeAttribute("b", "viz:x"));
        assertNull(graph.getNodeAttribute("b", "viz:y"));
        assertNull(graph.getNodeAttribute("b", "viz:size"));
        assertNull(graph.getNodeAttribute("c", "viz:size"));
    }

    @Test public void importsColorsAndIgnoresInvalidValues()
    {
        Graph<String> graph = read("<nodes xmlns:v='http://gexf.net/1.3/viz'>"
                + "<node id='a'><v:color r='12' g='34' b='56' a='0.5'/></node>"
                + "<node id='b'><v:color hex='#Ab1234'/></node>"
                + "<node id='c'><v:color r='256' g='0' b='0'/></node>"
                + "<node id='d'><v:color r='0' g='0' b='0'/></node></nodes>", false);
        assertNotNull(graph);
        assertEquals("rgba(12,34,56,0.5)", graph.getNodeAttribute("a", "viz:color"));
        assertEquals("#Ab1234", graph.getNodeAttribute("b", "viz:color"));
        assertNull(graph.getNodeAttribute("c", "viz:color"));
        assertEquals("rgba(0,0,0,1.0)", graph.getNodeAttribute("d", "viz:color"));
    }

    @Test public void importsParallelEdgeStylesIndependentlyOfWeight()
    {
        MultiGraph<String> graph = (MultiGraph<String>) read("<edges xmlns:v='http://gexf.net/1.3/viz'>"
                + "<edge source='a' target='b' weight='3'><v:color hex='#123456'/><v:thickness value='7'/></edge>"
                + "<edge source='a' target='b'><v:color r='1' g='2' b='3' a='0.5'/><v:thickness value='0'/></edge>"
                + "<edge source='b' target='c'><v:thickness value='NaN'/><v:color hex='invalid'/></edge>"
                + "</edges>", true);
        assertNotNull(graph);
        assertEquals("#123456", graph.getEdgeAttribute("a", "b", 0, "viz:color"));
        assertEquals("rgba(1,2,3,0.5)", graph.getEdgeAttribute("a", "b", 1, "viz:color"));
        assertEquals(7.0, graph.getEdgeAttribute("a", "b", 0, "viz:thickness"));
        assertEquals(0.0, graph.getEdgeAttribute("a", "b", 1, "viz:thickness"));
        assertEquals(3.0, graph.getEdgeWeights("a", "b").get(0), 0.0);
        assertNull(graph.getEdgeAttribute("b", "c", 0, "viz:thickness"));
        assertNull(graph.getEdgeAttribute("b", "c", 0, "viz:color"));
    }

    @Test public void detectsDirectionOverConstructorFlags()
    {
        String edges = "<edges><edge source='a' target='b'/></edges>";
        Graph<String> undirected = read(edges, false, "defaultedgetype='undirected'", true);
        assertNotNull(undirected);
        assertFalse(undirected.isDirected());
        assertTrue(undirected.containsEdge("b", "a"));
        Graph<String> directed = read(edges, false, "defaultedgetype='directed'", false);
        assertTrue(directed.isDirected());
        assertFalse(directed.containsEdge("b", "a"));
        assertFalse(read("", false, "defaultedgetype='undirected'", true).isDirected());
        assertFalse(read(edges, false, "", false).isDirected());
    }

    @Test public void detectsOverridesAndExpandsMixedAndMutualEdges()
    {
        Graph<String> graph = read("<edges><edge source='a' target='b' type='directed'/>"
                + "<edge source='b' target='c' weight='4' label='both'/>"
                + "<edge source='c' target='d' type='mutual'/></edges>", false,
                "defaultedgetype='undirected'", false);
        assertNotNull(graph);
        assertTrue(graph.isDirected());
        assertFalse(graph.containsEdge("b", "a"));
        assertTrue(graph.containsEdge("c", "b"));
        assertEquals(4.0, graph.getEdgeWeight("c", "b"), 0.0);
        assertEquals("both", graph.getEdgeAttribute("c", "b", "label"));
        assertTrue(graph.containsEdge("d", "c"));
        assertFalse(read("<edges><edge source='a' target='b' type='undirected'/></edges>",
                false, "defaultedgetype='directed'", true).isDirected());
        assertNull(read("", false, "defaultedgetype='invalid'", true));
    }

    @Test public void optionallyUsesLabelsAsIdsAndRemapsEndpoints()
    {
        String xml = "<gexf><graph><nodes><node id='0' label='Alice'/><node id='1' label='Bob'/>"
                + "<node id='2'/></nodes><edges><edge source='0' target='1'/><edge source='1' target='2'/>"
                + "</edges></graph></gexf>";
        Graph<String> graph = new GexfGraphReader<>(false, true, false, false, Parsers.sp, true)
                .read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(graph);
        assertTrue(graph.containsEdge("Alice", "Bob"));
        assertTrue(graph.containsEdge("Bob", "2"));
        assertFalse(graph.containsVertex("0"));
        String duplicate = xml.replace("label='Bob'", "label='Alice'");
        assertNull(new GexfGraphReader<>(false, true, false, false, Parsers.sp, true)
                .read(new ByteArrayInputStream(duplicate.getBytes(StandardCharsets.UTF_8))));
        Graph<String> original = new GexfGraphReader<>(false, true, false, false, Parsers.sp)
                .read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertTrue(original.containsEdge("0", "1"));
    }

    @Test public void builtInLabelsOverrideCustomLabels()
    {
        Graph<String> graph = read("<attributes class='node'>"
                + "<attribute id='0' title='label' type='string'><default>default</default></attribute>"
                + "</attributes><nodes><node id='a' label='Alice'><attvalues>"
                + "<attvalue for='0' value='custom'/></attvalues></node><node id='b'/></nodes>", false);
        assertNotNull(graph);
        assertEquals("Alice", graph.getNodeAttribute("a", "label"));
        assertEquals("default", graph.getNodeAttribute("b", "label"));
    }
}
