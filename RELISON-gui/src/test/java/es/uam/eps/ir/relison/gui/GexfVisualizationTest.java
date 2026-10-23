package es.uam.eps.ir.relison.gui;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.io.graph.GexfGraphReader;
import org.ranksys.formats.parsing.Parsers;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

public class GexfVisualizationTest
{
    @Test public void serializesImportedCoordinatesAndSizeWithFallbacks()
    {
        String xml = "<gexf xmlns='http://gexf.net/1.3' xmlns:viz='http://gexf.net/1.3/viz'>"
                + "<graph><nodes><node id='a'><viz:position x='-10' y='25'/><viz:size value='7'/>"
                + "</node><node id='b'><viz:position x='99'/></node></nodes></graph></gexf>";
        Graph<String> graph = new GexfGraphReader<>(false, true, false, false, Parsers.sp)
                .read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(graph);
        Map<String, Object> serialized = GraphSerializer.toGraphology(
                new GraphSession("test", graph, true, false, false, false));
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) serialized.get("nodes");
        for (Map<String, Object> node : nodes)
        {
            Map<String, Object> attrs = (Map<String, Object>) node.get("attributes");
            if ("a".equals(node.get("key")))
            {
                assertEquals(-10.0, attrs.get("x"));
                assertEquals(25.0, attrs.get("y"));
                assertEquals(7.0, attrs.get("size"));
            }
            else
            {
                assertEquals(100.0, Math.hypot(((Number) attrs.get("x")).doubleValue(),
                        ((Number) attrs.get("y")).doubleValue()), 0.001);
                assertEquals(4, attrs.get("size"));
            }
        }
    }
}
