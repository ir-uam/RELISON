package es.uam.eps.ir.relison.sna.metrics.attributes.edges.indiv;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.sna.metrics.IndividualEdgeAttributeMetric;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Counts graph edges for each value of an edge attribute. */
public class Count<U> implements IndividualEdgeAttributeMetric<U>
{
    @Override
    public double compute(Graph<U> graph, String attrName, Object value)
    {
        return graph.getAllNodes().mapToDouble(source -> graph.getAdjacentNodes(source).mapToDouble(target ->
                Objects.equals(graph.getEdgeAttribute(source, target, attrName), value) ? 1.0 : 0.0).sum()).sum();
    }

    @Override
    public Map<Object, Double> compute(Graph<U> graph, String attrName)
    {
        Map<Object, Double> values = new LinkedHashMap<>();
        graph.getAllNodes().forEach(source -> graph.getAdjacentNodes(source).forEach(target ->
        {
            Object value = graph.getEdgeAttribute(source, target, attrName);
            values.merge(value, 1.0, Double::sum);
        }));
        return values;
    }

    @Override
    public double averageValue(Graph<U> graph, String attrName)
    {
        Map<Object, Double> values = compute(graph, attrName);
        return values.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
}