/**
 * Global metric that depends on the type of the edges of the graph
 */
package es.uam.eps.ir.relison.sna.metrics;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.sna.edgegroups.simple.SimpleEdgePartition;

import java.util.Map;

public interface EdgeTypeMetric<U> extends GraphMetric<U>
{

    double compute(Graph<U> graph, SimpleEdgePartition<U> edgeGroup);


    /**
     * Computes the value of the metric for a single user.
     *
     * @param graph The graph.
     * @param comm  The relation between communities and nodes.
     * @param indiv Individual community
     *
     * @return the value of the metric.
     */
    double compute(Graph<U> graph, Communities<U> comm, int indiv);

    /**
     * Computes the value of the metric for all the users in the graph.
     *
     * @param graph The graph.
     * @param comm  The communities of the graph.
     *
     * @return A map relating the users with the values of the metric.
     */
    Map<Integer, Double> compute(Graph<U> graph, Communities<U> comm);

    /**
     * Computes the average value of the metric in the graph.
     *
     * @param graph The graph.
     * @param comm  the communities of the graph.
     *
     * @return the average value of the metric.
     */
    double averageValue(Graph<U> graph, Communities<U> comm);
}
