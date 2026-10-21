package es.uam.eps.ir.relison.edge;

import es.uam.eps.ir.relison.graph.attributes.AttributeType;
import es.uam.eps.ir.relison.graph.multigraph.fast.FastDirectedWeightedMultiGraph;
import es.uam.eps.ir.relison.sna.edge.EdgeAttributePartition;
import es.uam.eps.ir.relison.sna.edge.detection.attributes.EdgeAttributePartitionDetector;
import org.junit.Test;
import static org.junit.Assert.*;

public class EdgeAttributePartitionDetectorTest
{
    @Test
    public void numericBinsHandleBoundsMissingValuesAndParallelEdges()
    {
        FastDirectedWeightedMultiGraph<String> graph = new FastDirectedWeightedMultiGraph<>();
        graph.defineEdgeAttribute("score", AttributeType.DOUBLE);
        graph.addEdge("a", "b", 1.0); graph.addEdge("a", "b", 2.0);
        graph.addEdge("b", "c", 1.0); graph.addEdge("c", "d", 1.0); graph.addEdge("d", "e", 1.0);
        graph.setEdgeAttribute("a", "b", 0, "score", -0.1);
        graph.setEdgeAttribute("a", "b", 1, "score", 0.0);
        graph.setEdgeAttribute("b", "c", 0, "score", 9.999);
        graph.setEdgeAttribute("c", "d", 0, "score", 10.0);
        EdgeAttributePartition<String, Object> partition = new EdgeAttributePartitionDetector<String>("score", 10.0).detectEdgePartition(graph);
        assertEquals(4, partition.getNumPartitions());
        java.util.Map<String, Integer> sizes = new java.util.HashMap<>();
        partition.getPartitions().forEach(group -> sizes.put(String.valueOf(partition.getAttributeValue(group)), partition.getPartitionSize(group)));
        assertEquals(Integer.valueOf(1), sizes.get("[-10, 0)"));
        assertEquals(Integer.valueOf(2), sizes.get("[0, 10)"));
        assertEquals(Integer.valueOf(1), sizes.get("[10, 20)"));
        assertEquals(Integer.valueOf(1), sizes.get("null"));
        assertEquals(0.0, (Double) graph.getEdgeAttribute("a", "b", 1, "score"), 0.0);
    }

    @Test
    public void invalidWidthsAndNonnumericBinningAreRejected()
    {
        assertThrows(IllegalArgumentException.class, () -> new EdgeAttributePartitionDetector<String>("score", 0.0));
        assertThrows(IllegalArgumentException.class, () -> new EdgeAttributePartitionDetector<String>("score", Double.NaN));
        FastDirectedWeightedMultiGraph<String> graph = new FastDirectedWeightedMultiGraph<>();
        graph.defineEdgeAttribute("kind", AttributeType.CATEGORICAL);
        assertThrows(IllegalArgumentException.class, () -> new EdgeAttributePartitionDetector<String>("kind", 1.0).detectEdgePartition(graph));
    }
}
