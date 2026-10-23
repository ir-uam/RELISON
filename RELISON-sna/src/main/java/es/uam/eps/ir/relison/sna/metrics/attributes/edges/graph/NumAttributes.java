package es.uam.eps.ir.relison.sna.metrics.attributes.edges.graph;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.sna.metrics.EdgeAttributeMetric;
import es.uam.eps.ir.relison.sna.metrics.IndividualEdgeAttributeMetric;
import es.uam.eps.ir.relison.sna.metrics.attributes.edges.indiv.Count;

import java.util.Map;

public class NumAttributes<U> implements EdgeAttributeMetric<U> {
    @Override
    public double compute(Graph<U> graph, String attr_name) {
        Count<U> metric = new Count<>();
        Map<Object, Double> vals = metric.compute(graph, attr_name);
        return vals.size();
    }
}
