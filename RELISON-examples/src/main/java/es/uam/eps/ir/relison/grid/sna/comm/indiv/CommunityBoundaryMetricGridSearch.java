package es.uam.eps.ir.relison.grid.sna.comm.indiv;

import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.sna.metrics.IndividualCommunityMetric;
import es.uam.eps.ir.relison.sna.metrics.communities.indiv.*;
import java.util.*;
import java.util.function.Supplier;
import static es.uam.eps.ir.relison.grid.sna.comm.indiv.IndividualCommunityMetricIdentifiers.*;

/** Grid adapter for community boundary metrics, shared by GUI and experiment configuration. */
public class CommunityBoundaryMetricGridSearch<U> implements IndividualCommunityMetricGridSearch<U>
{
    private final String metric;

    public CommunityBoundaryMetricGridSearch(String metric)
    {
        this.metric = Objects.requireNonNull(metric);
    }

    @Override
    public Map<String, Supplier<IndividualCommunityMetric<U>>> grid(Grid grid)
    {
        boolean modularity = metric.equals(MODULARITYCONTRIBUTION);
        boolean supportsWeights = modularity || metric.equals(INTERNALVOLUMEFRACTION)
                || metric.equals(CONDUCTANCE) || metric.equals(EDGEEXPANSION);
        List<EdgeOrientation> orientations = new ArrayList<>();
        if (!modularity)
        {
            orientations.addAll(grid.getOrientationValues("orientation"));
            if (orientations.isEmpty())
                grid.getStringValues("orientation").forEach(value -> orientations.add(EdgeOrientation.valueOf(value)));
        }
        if (orientations.isEmpty()) orientations.add(EdgeOrientation.UND);
        List<Boolean> weights = supportsWeights ? grid.getBooleanValues("weighted") : Collections.emptyList();
        if (weights.isEmpty()) weights = List.of(false);
        List<Double> resolutions = modularity ? grid.getDoubleValues("resolution") : Collections.emptyList();
        if (resolutions.isEmpty()) resolutions = List.of(1.0);
        Map<String, Supplier<IndividualCommunityMetric<U>>> result = new LinkedHashMap<>();
        for (EdgeOrientation orientation : orientations)
            for (boolean weighted : weights)
                for (double resolution : resolutions)
                {
                    if (orientation != EdgeOrientation.UND && orientation != EdgeOrientation.IN && orientation != EdgeOrientation.OUT)
                        throw new IllegalArgumentException("Community boundary orientation must be OUT, IN or UND.");
                    if (!Double.isFinite(resolution) || resolution < 0)
                        throw new IllegalArgumentException("Modularity resolution must be finite and nonnegative.");
                    String key = metric + (modularity ? "" : "_" + orientation)
                            + (supportsWeights ? "_weighted=" + weighted : "")
                            + (modularity ? "_resolution=" + resolution : "");
                    result.put(key, () -> create(orientation, weighted, resolution));
                }
        return result;
    }

    private IndividualCommunityMetric<U> create(EdgeOrientation orientation, boolean weighted, double resolution)
    {
        switch (metric)
        {
            case BOUNDARYNODES: return new BoundaryNodes<>(orientation);
            case BOUNDARYNODERATIO: return new BoundaryNodeRatio<>(orientation);
            case NEIGHBORINGCOMMUNITIES: return new NeighboringCommunities<>(orientation);
            case INTERNALVOLUMEFRACTION: return new InternalVolumeFraction<>(orientation, weighted);
            case CONDUCTANCE: return new Conductance<>(orientation, weighted);
            case EDGEEXPANSION: return new EdgeExpansion<>(orientation, weighted);
            case MODULARITYCONTRIBUTION: return new ModularityContribution<>(weighted, resolution);
            default: throw new IllegalArgumentException("Unknown community boundary metric: " + metric);
        }
    }
}
