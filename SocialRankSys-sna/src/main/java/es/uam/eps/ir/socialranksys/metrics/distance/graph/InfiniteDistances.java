package es.uam.eps.ir.socialranksys.metrics.distance.graph;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.metrics.GraphMetric;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;

/**
 * Finds the number of infinite distance pairs of nodes.
 * @param <U> type of the users.
 */
public class InfiniteDistances<U> implements GraphMetric<U>
{
    /**
     * The distance calculator.
     */
    private final DistanceCalculator<U> calculator;

    /**
     * Constructor.
     * @param calculator a distance calculator.
     */
    public InfiniteDistances(DistanceCalculator<U> calculator)
    {
        this.calculator = calculator;
    }

    @Override
    public double compute(Graph<U> graph)
    {
        calculator.computeDistances(graph);
        return graph.getAllNodes().mapToDouble(node -> graph.getAllNodes().filter(target -> !target.equals(node)).filter(target -> !Double.isFinite(this.calculator.getDistances(node, target))).count() + 0.0).sum();
    }
}
