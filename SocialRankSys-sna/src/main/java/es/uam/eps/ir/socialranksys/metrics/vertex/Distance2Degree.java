package es.uam.eps.ir.socialranksys.metrics.vertex;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.metrics.VertexMetric;

import java.util.HashSet;
import java.util.Set;

public class Distance2Degree<U> implements VertexMetric<U>
{
    private final EdgeOrientation first;
    private final EdgeOrientation second;

    public Distance2Degree(EdgeOrientation first, EdgeOrientation second)
    {
        this.first = first;
        this.second = second.invertSelection();
    }

    @Override
    public double compute(Graph<U> graph, U user)
    {
        Set<U> d2neighs = new HashSet<>();
        graph.getNeighbourhood(user, first).forEach(neigh -> graph.getNeighbourhood(neigh, second).forEach(d2neighs::add));
        return d2neighs.size();
    }
}
