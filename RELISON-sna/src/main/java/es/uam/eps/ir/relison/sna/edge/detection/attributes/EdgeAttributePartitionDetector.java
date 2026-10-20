package es.uam.eps.ir.relison.sna.edge.detection.attributes;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.multigraph.MultiGraph;
import es.uam.eps.ir.relison.sna.edge.Edge;
import es.uam.eps.ir.relison.sna.edge.EdgeAttributePartition;
import es.uam.eps.ir.relison.sna.edge.detection.EdgePartitionDetector;
import es.uam.eps.ir.relison.sna.edge.detection.EdgePartitionSupport;

import java.util.LinkedHashMap;
import java.util.Map;

/** Partitions edges according to the value of one declared edge attribute. */
public class EdgeAttributePartitionDetector<U> implements EdgePartitionDetector<U>
{
    private final String attribute;

    public EdgeAttributePartitionDetector(String attribute)
    {
        if (attribute == null || attribute.trim().isEmpty())
            throw new IllegalArgumentException("Attribute name cannot be empty");
        this.attribute = attribute;
    }

    public String getAttribute()
    {
        return attribute;
    }

    @Override
    public EdgeAttributePartition<U, Object> detectEdgePartition(Graph<U> graph)
    {
        if (graph == null) return null;
        if (graph.getEdgeAttributeType(attribute) == null)
            throw new IllegalArgumentException("Unknown edge attribute: " + attribute);

        Map<Integer, Object> labels = new LinkedHashMap<>();
        Map<Object, Integer> reverseLabels = new LinkedHashMap<>();
        EdgeAttributePartition<U, Object> partition = new EdgeAttributePartition<>(labels, reverseLabels);
        EdgePartitionSupport.forEachEdge(graph, edge ->
        {
            Object value = getValue(graph, edge);
            Integer group = reverseLabels.get(value);
            if (group == null && !reverseLabels.containsKey(value))
            {
                group = labels.size();
                labels.put(group, value);
                reverseLabels.put(value, group);
                partition.addPartition();
            }
            partition.add(edge, group);
        });
        return partition;
    }

    private Object getValue(Graph<U> graph, Edge<U> edge)
    {
        if (graph instanceof MultiGraph<?>)
        {
            MultiGraph<U> multiGraph = (MultiGraph<U>) graph;
            return multiGraph.getEdgeAttribute(edge.getSource(), edge.getTarget(), edge.getIndex(), attribute);
        }
        return graph.getEdgeAttribute(edge.getSource(), edge.getTarget(), attribute);
    }
}
