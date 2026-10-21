package es.uam.eps.ir.relison.sna.metrics.communities.indiv;

import es.uam.eps.ir.relison.utils.ComputationMonitor;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.sna.metrics.IndividualCommunityMetric;
import java.util.*;

/** Shares a single graph traversal across all community values in a computation. */
abstract class AbstractCommunityBoundaryMetric<U> implements IndividualCommunityMetric<U>
{
    protected final EdgeOrientation orientation;
    private final boolean weighted;

    AbstractCommunityBoundaryMetric(EdgeOrientation orientation, boolean weighted)
    {
        if (orientation != EdgeOrientation.OUT && orientation != EdgeOrientation.IN && orientation != EdgeOrientation.UND)
            throw new IllegalArgumentException("Community boundary orientation must be OUT, IN or UND.");
        this.orientation = orientation;
        this.weighted = weighted;
    }

    protected abstract double evaluate(CommunityBoundaryStatistics<U> statistics, int group);

    @Override
    public double compute(Graph<U> graph, Communities<U> communities, int group)
    {
        if (group < 0 || group >= communities.getNumCommunities())
            throw new IllegalArgumentException("Invalid community: " + group);
        return evaluate(new CommunityBoundaryStatistics<>(graph, communities, weighted), group);
    }

    @Override
    public Map<Integer, Double> compute(Graph<U> graph, Communities<U> communities)
    {
        CommunityBoundaryStatistics<U> statistics = new CommunityBoundaryStatistics<>(graph, communities, weighted);
        Map<Integer, Double> values = ComputationMonitor.current().track(new LinkedHashMap<>(), "Computing community values", communities.getNumCommunities(), "communities");
        communities.getCommunities().forEach(group -> {
            if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
            values.put(group, evaluate(statistics, group));
        });
        return values;
    }

    /** Arithmetic mean; an undefined group value propagates as NaN, as in the individual metric contract. */
    @Override
    public double averageValue(Graph<U> graph, Communities<U> communities)
    {
        return compute(graph, communities).values().stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
    }
}
